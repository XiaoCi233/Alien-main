#version 150

#ifdef GL_ES
precision mediump float;
#endif
#define NUM_OCTAVES 16

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
uniform vec2 resolution;

out vec4 fragColor;

uniform vec3 color;
uniform vec3 color2;
uniform float mixFactor;

uniform float minAlpha;
uniform float quality;
uniform float radius;
uniform float divider;
uniform float maxSample;
uniform float time;
uniform float size;

float glowShader() {
    vec2 texelSize = vec2(1.0 / resolution.x * (radius * quality), 1.0 / resolution.y * (radius * quality));
    float alpha = 0;

    for (float x = -radius; x < radius; x++) {
        for (float y = -radius; y < radius; y++) {
            vec4 currentColor = texture(DiffuseSampler, texCoord + vec2(texelSize.x * x, texelSize.y * y));

            if (currentColor.a != 0)
            alpha += divider > 0 ? max(0.0, (maxSample - distance(vec2(x, y), vec2(0))) / divider) : 1;
            alpha *= minAlpha;
        }
    }

    return alpha;
}

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    float alpha = 0;
    float gradient = 0.5 + 0.5 * sin(time + texCoord.x * size); // 时间和横向位置影响渐变

    // 计算最终颜色
    vec3 animatedColor = mix(color, color2, gradient); // 颜色从1渐变到2
    if (centerCol.a != 0) {

        fragColor = vec4(animatedColor, mixFactor);
        //fragColor = vec4(color, mixFactor);
    } else {
        alpha = glowShader();
        fragColor = vec4(animatedColor, alpha);
    }
}
