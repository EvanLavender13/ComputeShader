#version 460 core

#define M_PI 3.1415926535897932384626433832795

layout (local_size_x = 256, local_size_y = 1, local_size_z = 1) in;

layout (binding = 0, rgba32f) uniform image2D AgentMap;
layout (binding = 1, rgba32f) uniform image2D AgentMapOut;
layout (binding = 2, rgba32f) uniform image2D TrailMap;
layout (binding = 3, rgba32f) uniform image2D TrailMapOut;

layout (binding = 4, std430) restrict buffer ShaderParameters {
    float frameBufferWidth;
    float frameBufferHeight;
    float sensingDistance;
    float sensingAngle;
    float turningAngle;
    float depositAmount;
    float decayAmount;
    float stepSize;
} params;

struct Agent {
    float x, y;
    float angle;
    float r, g, b;
};

layout (std430) restrict buffer Agents {
    Agent agents[];
};

uniform uint Stage;
uniform float Time;

// Hash function www.cs.ubc.ca/~rbridson/docs/schechter-sca08-turbulence.pdf
uint hash(uint state) {
    state ^= 2747636419u;
    state *= 2654435769u;
    state ^= state >> 16;
    state *= 2654435769u;
    state ^= state >> 16;
    state *= 2654435769u;
    return state;
}

float scaleToRange01(uint state) {
    return state / 4294967295.0;
}

// https://stackoverflow.com/a/17897228
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// All components are in the range [0…1], including hue.
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float sigmoid(float x) {
    return 1.0 / (1.0 + exp(x)); 
}

float sense(Agent agent, float sensorAngleOffset) {
    float sensorAngle = agent.angle + sensorAngleOffset;
    vec2 sensorDirection = vec2(cos(sensorAngle), sin(sensorAngle));

    vec2 position = vec2(agent.x, agent.y);
    float sensorDistance = params.sensingDistance;
    vec2 sensorPosition = position + sensorDirection * sensorDistance;
    uint sensorCenterX = uint(sensorPosition.x);
    uint sensorCenterY = uint(sensorPosition.y);

    vec4 color = vec4(agent.r, agent.g, agent.b, 0.0f);
    vec3 colorHsv = rgb2hsv(color.rgb);

    float sum = 0.0f;

    for (int offsetX = -1; offsetX <= 1; offsetX++) {
        for (int offsetY = -1; offsetY <= 1; offsetY++) {
            uint sampleX = uint(min(params.frameBufferWidth - 1, max(0, sensorCenterX + offsetX)));
            uint sampleY = uint(min(params.frameBufferHeight - 1, max(0, sensorCenterY + offsetY)));
            sum += rgb2hsv(imageLoad(TrailMap, ivec2(sampleX, sampleY)).rgb).x;
        }
    }
 
    return 0.0f;
}

// https://github.com/erlingpaulsen/godot-physarum/blob/main/physarum_compute_shader.glsl

void main() {
    uint id = uint(gl_GlobalInvocationID.x);
    
    Agent agent = agents[id];
    vec2 pos = vec2(agent.x, agent.y);

    uint width = uint(params.frameBufferWidth);
    uint height = uint(params.frameBufferHeight);
    uint random = hash(uint(pos.y * width + pos.x + hash(uint(id + Time * 100000))));

    float sensorAngle = params.sensingAngle;
    float weightForward = sense(agent, 0);
    float weightLeft = sense(agent, sensorAngle);
    float weightRight = sense(agent, -sensorAngle);



    /*
    uint gid = uint(gl_GlobalInvocationID.x);
    uint width = uint(params.frameBufferWidth);
    uint height = uint(params.frameBufferHeight);

    float t = 0.5f * (1 + sin(2 * M_PI * 1.0f + Time));

    if (Stage == 0) {
        float angle = agents[gid].angle;
        vec2 position = vec2(agents[gid].x, agents[gid].y);
        ivec2 pixel = ivec2(int(position.x), int(position.y));

        //float rnd = hash(pixel.y * width + pixel.x * height);
        float rnd = rand(angle);

        float sensingDistance = params.sensingDistance;
        float sensingAngle = params.sensingAngle / 2.0f;
        vec2 sensor_front = vec2(
            mod(position.x + sensingDistance * cos(angle), width), 
            mod(position.y + sensingDistance * sin(angle), height)
        );
        vec2 sensor_left = vec2(
            mod(position.x + sensingDistance * cos(angle + sensingAngle), width), 
            mod(position.y + sensingDistance * sin(angle + sensingAngle), height)
        );
        vec2 sensor_right = vec2(
            mod(position.x + sensingDistance * cos(angle - sensingAngle), width), 
            mod(position.y + sensingDistance * sin(angle - sensingAngle), height)
        );

        ivec2 sensor_front_pos = ivec2(int(sensor_front.x), int(sensor_front.y));
        ivec2 sensor_left_pos = ivec2(int(sensor_left.x), int(sensor_left.y));
        ivec2 sensor_right_pos = ivec2(int(sensor_right.x), int(sensor_right.y));

        // Read trail map values at sensor positions
        vec4 color = vec4(agents[gid].r, agents[gid].g, agents[gid].b, 0.0f);
        //vec4 color = vec4(rnd, rnd, rnd, 1.0f);
        vec4 sensor_front_color = imageLoad(TrailMap, sensor_front_pos);
        vec4 sensor_left_color = imageLoad(TrailMap, sensor_left_pos);
        vec4 sensor_right_color = imageLoad(TrailMap, sensor_right_pos);

        vec3 color_hsv = rgb2hsv(color.rgb);
        vec3 sensor_front_hsv = rgb2hsv(sensor_front_color.rgb);
        vec3 sensor_left_hsv = rgb2hsv(sensor_left_color.rgb);
        vec3 sensor_right_hsv = rgb2hsv(sensor_right_color.rgb);

        float sensor_front_v = abs(color_hsv.x - sensor_front_hsv.x);
        float sensor_left_v = abs(color_hsv.x - sensor_left_hsv.x);
        float sensor_right_v = abs(color_hsv.x - sensor_right_hsv.x);

        float turningAngle = params.turningAngle;
        if ((sensor_left_v < sensor_front_v) && (sensor_right_v < sensor_front_v)) {
            if (rnd < 0.5) {
                angle += turningAngle;
            } else {
                angle -= turningAngle;
            }  
        } else if (sensor_right_v > sensor_left_v) {
            angle += turningAngle * rnd;
        } else if (sensor_left_v > sensor_right_v) {
            angle -= turningAngle * rnd;
        } else {
            //angle += rnd;
        }

        float stepSize = params.stepSize;// + params.stepSize * t;
        vec2 new_position = vec2(
            mod(agents[gid].x + stepSize * cos(angle), width),
            mod(agents[gid].y + stepSize * sin(angle), height)
        );

        if (new_position.x <= 0 || new_position.x >= width || new_position.y <= 0 || new_position.y >= height) {
            //new_position.x = min(width - 1.0f, max(0.0f, new_position.x));
            //new_position.y = min(height - 1.0f, max(0.0f, new_position.y));
            new_position.x = mod(new_position.x, width);
            //new_position.x 
            //new_position.x = 1.0f + width * rnd;
            //if (rnd < 0.5f) {
            //    new_position.x += stepSize;
            //} else {
            //    new_position.x -= stepSize;
            //}
            t = new_position.x / width;
            // height * 0.25f + ...  * 0.5f
            new_position.y = height * (0.5f * (1.0f + sin(2.0f * M_PI * t * 1.0f + Time * 0.1f)));
            if (rnd < 0.5f) {
                new_position.y = mod(new_position.y, height);
            } else {
                new_position.y = mod(new_position.y, height);
            }            
            //angle -= 3.14f;
            //angle = -angle;
            angle = rnd * 2.0f * M_PI;         
        }

        ivec2 new_pixel = ivec2(int(new_position.x), int(new_position.y));

        vec4 AgentMapV = imageLoad(AgentMap, pixel);
        vec4 AgentMapNewV = imageLoad(AgentMap, new_pixel);
        vec4 TrailMapV = imageLoad(TrailMap, pixel);
        vec4 TrailMapNewV = imageLoad(TrailMap, new_pixel);

        float depositAmount = params.depositAmount;
        vec4 full_color = vec4(color.rgb, 1.0f);
        vec4 new_color = full_color * depositAmount;
        vec4 new_value = TrailMapV + new_color;

        imageStore(AgentMapOut, new_pixel, AgentMapNewV + 1.0f);
        imageStore(TrailMapOut, new_pixel, new_value);

        agents[gid].x = new_position.x;
        agents[gid].y = new_position.y;
        agents[gid].angle = angle;
    } else {
        if (gid >= width * height) {
            return;
        }

        int y = int(gid / params.frameBufferWidth);
        int x = int(mod(float(gid), width));
        int k = int(5 / 2);
        float n = pow(5, 2);
        vec4 sum = vec4(0.0f);
        for (int i = -k; i < k + 1; i++) {
            for (int j = -k; j < k + 1; j++) {
                ivec2 pos_k = ivec2(int(mod(float(x + i), width)), int(mod(float(y + j), height)));
                sum += imageLoad(TrailMap, pos_k);
            }
        }

        ivec2 pixel = ivec2(x, y);
        float decayAmount = 1.0f - params.decayAmount;
        vec4 v = sum / n;
        imageStore(TrailMapOut, pixel,  v * decayAmount);
        imageStore(AgentMapOut, pixel, vec4(0.0f));
    }
    */
}