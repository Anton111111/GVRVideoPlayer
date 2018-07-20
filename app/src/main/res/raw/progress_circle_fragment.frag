precision mediump float;

uniform vec4  u_Color;
uniform float u_Radius;
uniform float u_Thickness;
uniform float u_Percent;
uniform float u_SmoothFactor; //From 0 to 1
uniform vec3  u_Center;
varying vec3  v_Position;

void main() {
    float dPercent = u_Percent * 360.0 / 100.0;
    float degree = degrees(atan(v_Position.y, v_Position.x)) ;
    if (degree < (180.0 - dPercent)) {
        discard;
    } else {
        float w = u_Thickness / 2.0;
        float r = u_Radius - (u_Thickness / 2.0);
        vec3 xyz = v_Position - u_Center;
        float l = length(xyz);
        gl_FragColor = u_Color;
        float a = smoothstep(w / 2.0, 0.0, abs(l-r) - w);
        gl_FragColor.a = a;
    }
}