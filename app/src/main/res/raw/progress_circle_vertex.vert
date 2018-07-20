uniform mat4 u_MVP;
attribute vec4 a_Position;

varying vec3 v_Position;

void main() {
    v_Position = a_Position.xyz;
    gl_Position = u_MVP * a_Position;
}