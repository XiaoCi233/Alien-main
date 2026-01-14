#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
in vec2 oneTexel;
out vec4 fragColor;

uniform float quality;
uniform float radius;
uniform float divider;
uniform float maxSample;

uniform float alpha2;
uniform vec3 rgb;
uniform vec3 rgb1;
uniform vec3 rgb2;
uniform vec3 rgb3;

uniform float step;
uniform vec2 resolution;
uniform float time;

float glowShader() {

    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0)
            alpha += divider > 0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0))) / divider) : 1;
        }
    }

    return alpha;
}

vec3 getColor(vec4 centerCol) {
    float distance = sqrt(gl_FragCoord.x * gl_FragCoord.x + gl_FragCoord.y * gl_FragCoord.y) + time;
    float distance2 = sqrt((gl_FragCoord.x - 1920.0) * (gl_FragCoord.x - 1920.0) + gl_FragCoord.y * gl_FragCoord.y) + time;
    float distance3 = sqrt((gl_FragCoord.x - 1080.0) * (gl_FragCoord.x - 1080.0) + (gl_FragCoord.y - 1080.0) * (gl_FragCoord.y - 1080.0)) + time;

    distance = sin(distance / step + sin(gl_FragCoord.y / step)) * 0.5 + 0.5;
    distance2 = sin(distance2 / step + sin(gl_FragCoord.x / step)) * 0.5 + 0.5;
    distance3 = sin(distance3 / step + sin(gl_FragCoord.y / step + gl_FragCoord.x / step)) * 0.5 + 0.5;

    float ripple = sin(distance * 10 + step * 10000) * 0.05;
    float swirl = cos((gl_FragCoord.x - 960.0) * 0.05 + (gl_FragCoord.y - 540.0) * 0.05 + step) * 0.05;

    distance += ripple + swirl;
    distance2 += ripple - swirl;
    distance3 += ripple + swirl;

    float distanceInv = 1.0 - distance;
    float r = rgb.r * distance + rgb1.r * distanceInv + rgb2.r * distance2 + rgb3.r * distance3;
    float g = rgb.g * distance + rgb1.g * distanceInv + rgb2.g * distance2 + rgb3.g * distance3;
    float b = rgb.b * distance + rgb1.b * distanceInv + rgb2.b * distance2 + rgb3.b * distance3;
    return vec3(r, g, b);
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);

    if (centerCol.a != 0) {
        fragColor = vec4(getColor(centerCol), alpha2);
    } else {
        fragColor = vec4(getColor(centerCol), glowShader());
    }
}