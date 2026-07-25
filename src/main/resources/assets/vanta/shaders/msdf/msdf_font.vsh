#version 120

varying vec2 textureCoordinate;
varying vec4 fragmentColor;

void main() {
    textureCoordinate = gl_MultiTexCoord0.xy;
    fragmentColor = gl_Color;
    gl_Position = ftransform();
}
