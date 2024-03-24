#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float Offset;

out vec4 fragColor;

void main() {
    vec2 downCoord = texCoord;
    vec2 halfpixel = oneTexel * Offset;

    vec4 sum = texture(DiffuseSampler, downCoord + vec2(-halfpixel.x * 2.0, 0.0));
    sum += texture(DiffuseSampler, downCoord + vec2(-halfpixel.x, halfpixel.y)) * 2.0;
    sum += texture(DiffuseSampler, downCoord + vec2(0.0, halfpixel.y * 2.0));
    sum += texture(DiffuseSampler, downCoord + vec2(halfpixel.x, halfpixel.y)) * 2.0;
    sum += texture(DiffuseSampler, downCoord + vec2(halfpixel.x * 2.0, 0.0));
    sum += texture(DiffuseSampler, downCoord + vec2(halfpixel.x, -halfpixel.y)) * 2.0;
    sum += texture(DiffuseSampler, downCoord + vec2(0.0, -halfpixel.y * 2.0));
    sum += texture(DiffuseSampler, downCoord + vec2(-halfpixel.x, -halfpixel.y)) * 2.0;

    fragColor = sum / 12.0;
}