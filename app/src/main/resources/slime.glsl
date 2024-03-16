#version 460 core

#define M_PI 3.1415926535897932384626433832795

layout (local_size_x = 1024, local_size_y = 1, local_size_z = 1) in;

layout (binding = 0, rgba32f) uniform image2D AgentMap;
layout (binding = 1, rgba32f) uniform image2D AgentMapOut;
layout (binding = 2, rgba32f) uniform image2D TrailMap;
layout (binding = 3, rgba32f) uniform image2D TrailMapOut;

layout (std430) restrict buffer ShaderParameters {
    float frameBufferWidth;
    float frameBufferHeight;
    float sensingDistance;
    float sensingAngle;
    float turningAngle;
    float depositAmount;
    float diffuseAmount;
    float decayAmount;
    float stepSize;
    float randomAmount;
} params;

struct Agent {
    float x, y;
    float angle;
    float r, g, b;
};

layout (std430) restrict buffer AgentData {
    Agent agents[];
};

uniform uint Stage;
uniform float Time;
uniform float Delta;

float rand(float n) {
    return fract(sin(n) * 43758.5453123f);
}

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

vec4 sense(ivec2 position, vec4 color) {
    /*
    if (position.x > 0 && position.x < params.frameBufferWidth && position.y > 0 && position.y < params.frameBufferHeight) {
        return imageLoad(TrailMap, position);
    } else {
        return color;
    }
    */

    return imageLoad(TrailMap, position);
}

// https://github.com/erlingpaulsen/godot-physarum/blob/main/physarum_compute_shader.glsl

void main() {
    uint gid = uint(gl_GlobalInvocationID.x);
    uint width = uint(params.frameBufferWidth);
    uint height = uint(params.frameBufferHeight);

    //float t = 0.5f * (1 + sin(2 * M_PI * 1.0f + Time));

    if (Stage == 0) {
        float angle = agents[gid].angle;
        vec2 position = vec2(agents[gid].x, agents[gid].y);
        ivec2 pixel = ivec2(int(position.x), int(position.y));

        float rnd = scaleToRange01(hash(pixel.y * width + pixel.x + hash(gid + int(Time) * 100000)));

        float sensingDistance = params.sensingDistance;
        float sensingAngle = params.sensingAngle;
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

        vec3 color = vec3(agents[gid].r, agents[gid].g, agents[gid].b);
        vec4 sensor_front_color = imageLoad(TrailMap, sensor_front_pos);
        vec4 sensor_left_color = imageLoad(TrailMap, sensor_left_pos);
        vec4 sensor_right_color = imageLoad(TrailMap, sensor_right_pos);

        vec3 color_hsv = rgb2hsv(color);
        vec3 sensor_front_hsv = rgb2hsv(sensor_front_color.rgb);
        vec3 sensor_left_hsv = rgb2hsv(sensor_left_color.rgb);
        vec3 sensor_right_hsv = rgb2hsv(sensor_right_color.rgb);

        float sensor_front_v = pow(abs(color_hsv.x - sensor_front_hsv.x), 2);
        float sensor_left_v = pow(abs(color_hsv.x - sensor_left_hsv.x), 2);
        float sensor_right_v = pow(abs(color_hsv.x - sensor_right_hsv.x), 2);

        float turningAngle = params.turningAngle;
        float randomAmount = params.randomAmount;
        if ((sensor_left_v < sensor_front_v) && (sensor_right_v < sensor_front_v)) {
            angle += sign(rnd - 0.5f) * (turningAngle - turningAngle * (1.0f - randomAmount));
        } else if (sensor_left_v < sensor_right_v) {
            angle += (turningAngle - turningAngle * (1.0f - rnd) * randomAmount);
        } else if (sensor_right_v < sensor_left_v) {
            angle -= (turningAngle - turningAngle * (1.0f - rnd) * randomAmount);
        } else {
            angle += (rnd - 0.5f) * 4.0f * M_PI * randomAmount;
        }

        float stepSize = params.stepSize * Delta;
        vec2 new_position = vec2(
            mod(agents[gid].x + stepSize * cos(angle), width),
            mod(agents[gid].y + stepSize * sin(angle), height)
        );

        ivec2 new_pixel = ivec2(int(new_position.x), int(new_position.y));

        vec4 AgentMapV = imageLoad(AgentMap, pixel);
        vec4 AgentMapNewV = imageLoad(AgentMap, new_pixel);
        vec4 TrailMapV = imageLoad(TrailMap, pixel);
        vec4 TrailMapNewV = imageLoad(TrailMap, new_pixel);

        float depositAmount = params.depositAmount;
        vec4 full_color = vec4(color, 1.0f);
        vec4 new_color = full_color * depositAmount;
        vec4 new_value = min(TrailMapV + new_color * Delta, 1.0f);

        imageStore(AgentMapOut, new_pixel, AgentMapNewV + 1.0f);
        imageStore(TrailMapOut, new_pixel, new_value);

        agents[gid].x = new_position.x;
        agents[gid].y = new_position.y;
        agents[gid].angle = angle;
    } else {
        /*
        if (gid >= width * height) {
            return;
        }

        int x = int(mod(gid, width));
        int y = int(gid / width);
        ivec2 coord = ivec2(x, y);

        vec4 sum = vec4(0.0f);
        vec4 trailColor = imageLoad(TrailMap, coord);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                uint sampleX = min(width - 1, max(0, x + offsetX));
                uint sampleY = min(height - 1, max(0, y + offsetY));
                sum += imageLoad(TrailMap, ivec2(sampleX, sampleY));
            }
        }

        vec4 blurredColor = sum / 9.0f;
        float diffuseAmount = params.diffuseAmount;
        blurredColor = trailColor * (1.0f - diffuseAmount) + blurredColor * (diffuseAmount);
        imageStore(TrailMapOut, coord, max(blurredColor - (params.decayAmount) * Delta, 0.0f));
        */
        
        if (gid >= width * height) {
            return;
        }

        int x = int(mod(gid, width));
        int y = int(gid / width);
        ivec2 coord = ivec2(x, y);

        vec4 sum = vec4(0.0f);
        vec4 trailColor = imageLoad(TrailMap, coord);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                uint sampleX = min(width - 1, max(0, x + offsetX));
                uint sampleY = min(height - 1, max(0, y + offsetY));
                sum += imageLoad(TrailMap, ivec2(sampleX, sampleY));
            }
        }

        float decayAmount = 1.0f - (params.decayAmount * Delta);
        vec4 blurredColor = sum / 9.0f;
        float diffuseAmount = params.diffuseAmount;
        blurredColor = trailColor * (1.0f - diffuseAmount) + blurredColor * (diffuseAmount);
        //imageStore(TrailMapOut, coord, max(blurredColor - (params.decayAmount) * Delta, 0.0f));
        imageStore(TrailMapOut, coord,  blurredColor * decayAmount);
        imageStore(AgentMapOut, coord, vec4(0.0f));
        
    }
}