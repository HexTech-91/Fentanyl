#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

uniform mat4 ProjMat;

uniform float quality;
uniform float radius;
uniform float divider;
uniform float maxSample;

uniform float mixFactor;
uniform float minAlpha;
uniform vec3 color;
uniform vec3 color2;
uniform float time;
uniform float size;
uniform vec2 resolution;

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

void main() {
    vec4 centerCol = texture(DiffuseSampler, texCoord);
    
    float pulse = sin(time * size) * 0.5 + 0.5;
    float alpha = minAlpha + (mixFactor - minAlpha) * pulse;
    
    if (centerCol.a != 0) {
        vec3 finalColor = mix(color, color2, pulse);
        fragColor = vec4(finalColor, alpha);
    } else {
        float glow = glowShader() * alpha;
        vec3 finalColor = mix(color, color2, pulse);
        fragColor = vec4(finalColor, glow);
    }
}