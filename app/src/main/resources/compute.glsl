#version 460 core

layout (local_size_x = 128, local_size_y = 1, local_size_z = 1) in;

layout (binding = 0, rgba32f) uniform image2D agentMap;
layout (binding = 1, rgba32f) uniform image2D agentMapOut;
layout (binding = 2, rgba32f) uniform image2D trailMap;
layout (binding = 3, rgba32f) uniform image2D trailMapOut;

layout (binding = 4, std430) restrict buffer Parameters {
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

layout (binding = 5, std430) restrict buffer Agents {
    Agent agents[];
};

uniform uint stage;

float rand(float n) {
    return fract(sin(n) * 43758.5453123);
}

float hash(uint state) {
    state ^= 2747636419u;
    state *= 2654435769u;
    state ^= state >> 16;
    state *= 2654435769u;
    state ^= state >> 16;
    state *= 2654435769u;
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

// https://github.com/erlingpaulsen/godot-physarum/blob/main/physarum_compute_shader.glsl

void main() {
    uint gid = uint(gl_GlobalInvocationID.x);
    uint width = uint(params.frameBufferWidth);
    uint height = uint(params.frameBufferHeight);

    if (stage == 0) {
        float angle = agents[gid].angle;
        vec2 position = vec2(agents[gid].x, agents[gid].y);
        ivec2 pixel = ivec2(int(position.x), int(position.y));

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

        // Read trail map values at sensor positions
        vec4 sensor_front_color = imageLoad(trailMap, sensor_front_pos);
        vec4 sensor_left_color = imageLoad(trailMap, sensor_left_pos);
        vec4 sensor_right_color = imageLoad(trailMap, sensor_right_pos);

        vec3 sensor_front_hsv = rgb2hsv(sensor_front_color.rgb);
        vec3 sensor_left_hsv = rgb2hsv(sensor_left_color.rgb);
        vec3 sensor_right_hsv = rgb2hsv(sensor_right_color.rgb);

        vec4 color = vec4(agents[gid].r, agents[gid].g, agents[gid].b, 0.0f);
        vec3 color_hsv = rgb2hsv(color.rgb);
        float sensor_front_v = abs(color_hsv.x - sensor_front_hsv.x);
        float sensor_left_v = abs(color_hsv.x - sensor_left_hsv.x);
        float sensor_right_v = abs(color_hsv.x - sensor_right_hsv.x);

        float rnd = hash(gid);
        float turningAngle = params.turningAngle;
        
        if ((sensor_left_v < sensor_front_v) && (sensor_right_v < sensor_front_v)) {
            if (rnd < 0.5) {
                angle += turningAngle;
            } else {
                angle -= turningAngle;
            }  
        } else if (sensor_right_v > sensor_left_v) {
            angle += turningAngle;
        } else if (sensor_left_v > sensor_right_v) {
            angle -= turningAngle;
        }

        float stepSize = params.stepSize;
        vec2 new_position = vec2(
            mod(agents[gid].x + stepSize * cos(angle), width),
            mod(agents[gid].y + stepSize * sin(angle), height)
        );
        ivec2 new_pixel = ivec2(int(new_position.x), int(new_position.y));

        vec4 agentMapV = imageLoad(agentMap, pixel);
        vec4 agentMapNewV = imageLoad(agentMap, new_pixel);
        vec4 trailMapV = imageLoad(trailMap, pixel);
        vec4 trailMapNewV = imageLoad(trailMap, new_pixel);

        float depositAmount = params.depositAmount;
        vec4 full_color = vec4(color.rgb, 1.0f);
        vec4 new_color = full_color * depositAmount;
        vec4 new_value = trailMapV + new_color;

        imageStore(agentMapOut, new_pixel, agentMapNewV + 1.0f);
        imageStore(trailMapOut, new_pixel, new_value);

        agents[gid].x = new_position.x;
        agents[gid].y = new_position.y;
        agents[gid].angle = angle;
    } else {
        if (gid >= width * height) {
            return;
        }

        int y = int(gid / params.frameBufferWidth);
        int x = int(mod(float(gid), width));
        int k = int(3 / 2);
        float n = pow(3, 2);
        vec4 sum = vec4(0.0f);
        for (int i = -k; i < k + 1; i++) {
            for (int j = -k; j < k + 1; j++) {
                ivec2 pos_k = ivec2(int(mod(float(x + i), width)), int(mod(float(y + j), height)));
                sum += imageLoad(trailMap, pos_k);
            }
        }

        ivec2 pixel = ivec2(x, y);
        float decayAmount = 1.0f - params.decayAmount;
        vec4 v = sum / n;
        imageStore(trailMapOut, pixel,  v * decayAmount);
        imageStore(agentMapOut, pixel, vec4(0.0f));
    }
}