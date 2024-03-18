#version 460 core

#define FLT_MAX 3.402823466e+38
#define FLT_MIN 1.175494351e-38
#define M_PI 3.1415926535897932384626433832795
#define EPSILON 1e-8;

float LengthSquared(in vec3 v) {
    return v.x * v.x + v.y * v.y + v.z * v.z;
}

bool NearZero(in vec3 v) {
    return true;
}

// PCG (permuted congruential generator). Thanks to:
// www.pcg-random.org and www.shadertoy.com/view/XlGcRh
uint NextRandom(inout uint state) {
    state = state * 747796405 + 2891336453;
    uint result = ((state >> ((state >> 28) + 4)) ^ state) * 277803737;
    result = (result >> 22) ^ result;
    return result;
}

float RandomValue(inout uint state) {
    return NextRandom(state) / 4294967295.0; // 2^32 - 1
}

float RandomValue(inout uint state, float min, float max) {
    return min + (max - min) * RandomValue(state);
}

vec3 RandomVec3(inout uint state) {
    return vec3(RandomValue(state), RandomValue(state), RandomValue(state));
}

vec3 RandomVec3(inout uint state, float min, float max) {
    return vec3(RandomValue(state, min, max), RandomValue(state, min, max), RandomValue(state, min, max));
}

vec3 RandomInUnitSphere(inout uint state) {
    while (true) {
        vec3 r = RandomVec3(state, -1.0f, 1.0f);
        if (LengthSquared(r) < 1.0f) {
            return r;
        }
    }
}

vec3 RandomUnitVector(inout uint state) {
    return normalize(RandomInUnitSphere(state));
}

vec3 RandomOnHemisphere(inout uint state, vec3 normal) {
    vec3 u = RandomUnitVector(state);
    if (dot(u, normal) > 0.0f) {
        return u;
    } else {
        return -u;
    }
}

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
    float refraction;
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
uniform uint Samples;

struct Ray {
    vec3 origin;
    vec3 direction;
};

vec3 RayAt(in Ray ray, float t) {
    return ray.origin + t * ray.direction;
}

struct HitRecord {
    vec3 point;
    vec3 normal;
    uint materialIndex;
    float t;
};

struct Interval {
    float min, max;
};

bool Contains(in Interval i, float v) {
    return i.min <= v && v <= i.max;
}

bool Surrounds(in Interval i, float v) {
    return i.min < v && v < i.max;
}

float Clamp(in Interval i, float v) {
    if (v < i.min) {
        return i.min;
    }

    if (v > i.max) {
        return i.max;
    }

    return v;
}

bool HitSphere(in Sphere sphere, in Ray ray, in Interval tInt, inout HitRecord record) {
    vec3 center = vec3(sphere.x, sphere.y, sphere.z);
    vec3 oc = ray.origin - center;
    float a = LengthSquared(ray.direction);
    float b = dot(oc, ray.direction);
    float c = LengthSquared(oc) - sphere.radius * sphere.radius;
    float disc = b * b - a * c;
    if (disc < 0.0f) {
        return false;
    }
    
    float root = (-b - sqrt(disc)) / a;
    if (!Surrounds(tInt, root)) {
        (-b + sqrt(disc)) / a;
        if (!Surrounds(tInt, root)) {
            return false;
        }
    }

    record.t = root;
    record.point = RayAt(ray, record.t);
    record.normal = (record.point - center) / sphere.radius;
    record.materialIndex = uint(sphere.materialIndex);

    return true;
}

bool HitAnything(in Ray ray, in Interval tInt, inout HitRecord record) {
    HitRecord r;
    bool hit = false;
    float closest = tInt.max;

    for (int i = 0; i < Spheres.length(); i++) {
        Sphere sphere = Spheres[i];
        if (HitSphere(sphere, ray, Interval(tInt.min, closest), r)) {
            hit = true;
            closest = r.t;
            record = r;
        }
    }

    return hit;
}

vec3 LambertianScatter(in HitRecord record, inout uint rngState) {
    vec3 direction = record.normal + RandomUnitVector(rngState);
    return direction;
}

vec3 RayColor(in Ray ray, inout uint rngState) {
    Ray currentRay = ray;
    vec3 attenuation = vec3(1.0f);

    int numBounces = 50;
    for (int i = 0; i < numBounces; i++) {
        HitRecord record;

        if (HitAnything(currentRay, Interval(0.001f, FLT_MAX), record)) {
            Material material = Materials[record.materialIndex];
            attenuation *= vec3(material.r, material.g, material.b);

            vec3 diffuse = record.normal + RandomUnitVector(rngState);
            currentRay.origin = record.point;
            currentRay.direction = diffuse;
            // vec3 reflect = reflect(currentRay.direction, record.normal) + RandomUnitVector(rngState) * (1.0f - material.metallic);
            // currentRay = Ray(record.point, mix(reflect, diffuse, material.roughness));

            // vec3 diffuse = record.normal + RandomUnitVector(rngState);
            // vec3 reflect = reflect(currentRay.direction, record.normal) + RandomUnitVector(rngState) * material.roughness;
            // currentRay = Ray(record.point, mix(diffuse, reflect, material.metallic));            
        } else {
            vec3 unitDirection = normalize(ray.direction);
            float t = 0.5f * (unitDirection.y + 1.0f);
            vec3 color = (1.0f - t) * vec3(1.0f) + t * vec3(0.5f, 0.7f, 1.0f);
            return color * attenuation;
        }
    }
    
    return vec3(0.0f);
}

void main() {
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    uint pixelIndex = gid.y * uint(Params.width) + gid.x;
    uint rngState = uint(pixelIndex);

    // ivec2 index = ivec2(gid.x, Params.height - gid.y);
    float aspectRatio = Params.width / Params.height;

    vec3 color = vec3(0.0f);
    for (int i = 0; i < Samples; i++) {
        float xCoord = (gid.x + (RandomValue(rngState) - 1.0f)) / Params.width * 2.0f - 1.0f;
        float yCoord = ((gid.y + (RandomValue(rngState) - 1.0f)) / Params.height * 2.0f - 1.0f) / aspectRatio;
        vec2 coord = vec2(xCoord, yCoord);

        vec4 target = InvProjection * vec4(coord, 1.0f, 1.0f);
        Ray ray = Ray(CameraPosition, vec3(InvView * vec4(normalize(vec3(target) / target.w), 0.0f)));
        color += RayColor(ray, rngState);
    }

    imageStore(Image, gid, vec4(sqrt(color / Samples), 1.0f));
}