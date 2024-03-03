#version 460 core

uniform sampler2D inputTexture;

in vec2 textureCoord;
out vec4 color;

void main() {
    color = texture(inputTexture, textureCoord);
    //color = vec4(1, 0, 0, 1);
}