#version 460 core

out vec2 textureCoord;

void main() {
    vec2 vertex = vec2(-1.0) + vec2(
        float((gl_VertexID & 1) << 2),
        float((gl_VertexID & 2) << 1)
    );

    gl_Position = vec4(vertex, 0.0, 1.0);
    textureCoord = vertex * 0.5 + vec2(0.5, 0.5);
}