#version 460 core

layout (local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

layout (binding = 0, rgba32f) uniform image2D Display;

layout (std430) restrict buffer ShaderParameters {
    float width;
    float height;
} params;

struct Object {
    float x, y;
    float dx, dy;
    float mass;
};

layout (std430) restrict buffer ObjectData {
    Object objects[];
};

uniform uint Stage;

float sigmoid(float x) {
    float v = 1.0 / (1.0 + exp(x));
    return 2.0f * (0.5f - v);
}

void main() {
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    vec2 pixel = vec2(gid);

    if (Stage == 0) {
        int i = gid.x;

        Object a = objects[i];
        float mass1 = a.mass;
        vec2 position1 = vec2(a.x, a.y);
        vec2 totalForce = vec2(0.0f);

        for (int j = 0; j < objects.length(); j++) {
            if (i == j || i == 0) continue;

            Object b = objects[j];
            float mass2 = b.mass;
            vec2 position2 = vec2(b.x, b.y);
            vec2 difference = position1 - position2;
            float distanceSquared = max(0.0f, pow(length(difference), 2));
            float force = 1.0f * (mass1 * mass2) / distanceSquared;

            totalForce += normalize(-difference) * force;
        }

        objects[i].dx += totalForce.x;
        objects[i].dy += totalForce.y;
    } else if (Stage == 1) { 
        int i = gid.x;

        objects[i].x += objects[i].dx * 0.01f;
        objects[i].y += objects[i].dy * 0.01f;        
    } else if (Stage == 2) {
        float mass1 = 1.0f;

        float totalForce = 0.0f;
        for (int i = 0; i < objects.length(); i++) {
            Object object = objects[i];
            float mass2 = object.mass;
            vec2 position = vec2(object.x, object.y);
            vec2 difference = pixel - position;
            float distanceSquared = pow(length(difference), 2);

            totalForce += (mass1 * mass2) / distanceSquared;
        }

        totalForce = sigmoid(totalForce);
        imageStore(Display, gid, vec4(1.0f, 1.0f, 1.0f, totalForce));
    }
}