#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform float Offset;

out vec4 fragColor;

void main() {
    vec2 downCoord = texCoord;
    vec2 halfpixel = oneTexel * Offset;

    vec4 sum = texture(DiffuseSampler, downCoord) * 4.0;
    sum += texture(DiffuseSampler, downCoord - halfpixel.xy);
    sum += texture(DiffuseSampler, downCoord + halfpixel.xy);
    sum += texture(DiffuseSampler, downCoord + vec2(halfpixel.x, -halfpixel.y));
    sum += texture(DiffuseSampler, downCoord - vec2(halfpixel.x, -halfpixel.y));

    fragColor = sum / 8.0;
}