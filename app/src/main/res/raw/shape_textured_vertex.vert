uniform mat4 u_MVP;
attribute vec4 a_Position;
attribute vec2 a_TexCoordinate;

varying vec2 v_TexCoordinate;

void main() {
   gl_Position = u_MVP * a_Position;
   v_TexCoordinate = a_TexCoordinate;
}
