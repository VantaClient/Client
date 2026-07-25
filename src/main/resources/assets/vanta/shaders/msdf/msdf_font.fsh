#version 120

uniform sampler2D Sampler0;
uniform float Range;
uniform vec2 AtlasSize;
uniform float Thickness;

varying vec2 textureCoordinate;
varying vec4 fragmentColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    float distance = median(texture2D(Sampler0, textureCoordinate).rgb) - 0.5 + Thickness;
    vec2 derivative = vec2(dFdx(textureCoordinate.x), dFdy(textureCoordinate.y)) * AtlasSize;
    float pixels = Range * inversesqrt(dot(derivative, derivative));
    float alpha = smoothstep(-0.5, 0.5, distance * pixels);
    gl_FragColor = vec4(fragmentColor.rgb, fragmentColor.a * alpha);
}
