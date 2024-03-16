#version 460 core

layout (local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

layout (binding = 0, rgba32f) uniform image2D Image;
//layout (binding = 1, rgba32f) uniform image2D ImageOut;

layout (std430) restrict buffer ShaderParameters {
    float width;
    float height;
} params;

uniform vec3 CameraPosition;
uniform mat4 InvProjection;
uniform mat4 InvView;

vec4 perPixel(vec2 coord) {
    vec4 target = InvProjection * vec4(coord.x, coord.y, 1.0f, 1.0f);
    vec3 rayDirection = vec3(InvView * vec4(normalize(vec3(target) / target.w), 0.0f));
    //vec3 origin = vec3(0.0f, 0.0f, 1.0f);
    //vec3 rayDirection = vec3(coord.x, coord.y, -1.0f);

    float radius = 0.5f;
    float a = dot(rayDirection, rayDirection);
    float b = 2.0f * dot(CameraPosition, rayDirection);
    float c = dot(CameraPosition, CameraPosition) - radius * radius;

    float discriminant = b * b - 4.0f * a * c;
    if (discriminant < 0.0f) {
        return vec4(0.0f, 0.0f, 0.0f, 1.0f);
    }

    float t0 = (-b - sqrt(discriminant)) / (2.0f * a);
    vec3 hitPoint = CameraPosition + rayDirection * t0;
    vec3 normal = normalize(hitPoint);

    vec3 lightDirection = normalize(vec3(-1.0f, -1.0f, -1.0f));
    float lightIntensity = max(dot(normal, -lightDirection), 0.0f);

    vec3 color = vec3(1.0f, 0.0f, 1.0f) * lightIntensity;

    return vec4(color, 1.0f);
}

void main() {
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 index = ivec2(gid.x, params.height - gid.y);

    float xCoord = index.x / params.width * 2.0f - 1.0f;
    float yCoord = index.y / params.height * 2.0f - 1.0f;

    imageStore(Image, index, perPixel(vec2(xCoord, yCoord)));
}