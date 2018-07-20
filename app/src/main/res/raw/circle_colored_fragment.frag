precision mediump float;

uniform vec4  u_Color;
uniform float u_Radius;
uniform float u_Thickness;
uniform vec3  u_Center;
varying vec3  v_Position;

void main() {
    float w = u_Thickness / 2.0;
    float r = u_Radius - (u_Thickness / 2.0);
    vec3 xyz = v_Position - u_Center;
    float l = length(xyz);
    gl_FragColor = u_Color;
    float a = smoothstep(w / 2.0, 0.0, abs(l-r) - w);
    gl_FragColor.a = a;
}