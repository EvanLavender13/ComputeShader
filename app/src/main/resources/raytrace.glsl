#version 460 core

#define FLT_MAX 3.402823466e+38
#define FLT_MIN 1.175494351e-38
#define M_PI 3.1415926535897932384626433832795

layout (local_size_x = 64, local_size_y = 1, local_size_z = 1) in;

layout (binding = 0, rgba32f) uniform image2D Image;
//layout (binding = 1, rgba32f) uniform image2D ImageOut;

layout (std430) restrict buffer ShaderParameters {
    float width;
    float height;
} Params;

struct Material {
    float r, g, b;
    float roughness;
    float metallic;
};

layout (std430) restrict buffer MaterialParameters {
    Material Materials[];
};

struct Sphere {
    float x, y, z;
    float radius;
    float materialIndex;
};

layout (std430) restrict buffer SphereParameters {
    Sphere Spheres[];
};

uniform vec3 CameraPosition;
uniform mat4 InvProjection;
uniform mat4 InvView;
uniform float Time;

struct Ray {
    vec3 origin;
    vec3 direction;
};

struct HitRecord {
    float t;
    vec3 position;
    vec3 normal;

    int objectIndex;
};

HitRecord Miss(Ray ray) {
    HitRecord record;
    record.t = -1.0f;
    return record;
}

HitRecord ClosestHit(Ray ray, float t, int objectIndex) {
    HitRecord record;
    record.t = t;
    record.objectIndex = objectIndex;

    Sphere sphere = Spheres[objectIndex];
    vec3 position = vec3(sphere.x, sphere.y, sphere.z);

    vec3 origin = ray.origin - position;
    record.position = origin + ray.direction * t;
    record.normal = (record.position - position) / sphere.radius;
    // record.normal = normalize(record.position);
    //record.position += position;

    return record;
}

HitRecord TraceRay(Ray ray, float tMin, float tMax) {
    int closestObject = -1;
    float hitDistance = FLT_MAX;
    for (int i = 0; i < Spheres.length(); i++) {
        Sphere sphere = Spheres[i];
        vec3 position = vec3(sphere.x, sphere.y, sphere.z);
        vec3 origin = ray.origin - position;

        float a = dot(ray.direction, ray.direction);
        float b = 2.0f * dot(origin, ray.direction);
        float c = dot(origin, origin) - sphere.radius * sphere.radius;

        float discriminant = b * b - 4.0f * a * c;
        if (discriminant < 0.0f) {
            continue;
        }

        float sqrtDiscriminant = sqrt(discriminant);
        float t = (-b - sqrtDiscriminant) / (2.0f * a);
        // if (t <= tMin || t >= tMax) {
        //     t = (-b + sqrtDiscriminant) / (2.0f * a);
        //     if (t <= tMin || t >= tMax) {
        //         continue;
        //     }
        // }

        if (t > 0.0f && t < hitDistance) {
            hitDistance = t;
            closestObject = i;
        }

        if (closestObject < 0) {
            return Miss(ray);
        }

        return ClosestHit(ray, t, closestObject);
    }
}

vec3 PerPixel(float xCoord, float yCoord, inout uint rngState) {
    int numSamples = 10;
    int numBounces = 10;
    vec3 totalColor = vec3(0.0f);
    for (int i = 0; i < numSamples; i++) {
        Ray ray;
        ray.origin = CameraPosition;
        vec2 coord = vec2(xCoord, yCoord);
        vec4 target = InvProjection * vec4(coord, 1.0f, 1.0f);
        vec3 jitter = RandomDirection(rngState) * 0.5f / Params.width;
        target.xyz += jitter;

        ray.direction = vec3(InvView * vec4(normalize(vec3(target) / target.w), 0.0f));

        float closest = FLT_MAX;
        vec3 color = vec3(1.0f);
        vec3 incoming = vec3(0.0f);
        for (int j = 0; j < numBounces; j++) {
            HitRecord record = TraceRay(ray, 0.0001f, closest);
            if (record.t > 0.0f) {
                Sphere sphere = Spheres[record.objectIndex];
                Material material = Materials[int(sphere.materialIndex)];

                vec3 materialColor = vec3(material.r, material.g, material.b);
                color *= materialColor;

                ray.origin = record.position + record.normal * 0.0001f;
                ray.direction = randomHemispherePoint(record.normal, randvec2(j));
            } else {
                // float a = 0.5f * (normalize(ray.direction).y + 1.0f);
                // vec3 env = ((1.0f - a) * vec3(1.0f) + a * vec3(0.5f, 0.7f, 1.0f));
                vec3 env = vec3(0.5f);
                incoming += env * color;
                break;
            }
        }

        totalColor += incoming;
    }

    return totalColor / numSamples;
}

void main() {
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 index = ivec2(gid.x, Params.height - gid.y);
    px = index;
    // uint rngState = int(index.y * Params.width + index.x + Time * 9384934);
    uint rngState = uint(Params.width * Time);

    float aspectRatio = Params.width / Params.height;
    float xCoord = index.x / Params.width * 2.0f - 1.0f;
    float yCoord = (index.y / Params.height * 2.0f - 1.0f) / aspectRatio;

    vec4 color = vec4(PerPixel(xCoord, yCoord, rngState), 1.0f);
    imageStore(Image, index, color);
}