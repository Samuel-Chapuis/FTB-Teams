package dev.ftb.mods.ftbteams.client.gui;

import java.util.ArrayList;
import java.util.List;

/** Camera-space geometry, independent of Minecraft/OpenGL so the cut plane can be regression-tested. */
public final class PreviewGeometry {
	private PreviewGeometry() {
	}

	public record Vertex(float x, float y, float z, float u, float v, float red, float green, float blue, float alpha) {
		public Vertex interpolate(Vertex other, float t) {
			return new Vertex(lerp(x, other.x, t), lerp(y, other.y, t), lerp(z, other.z, t),
					lerp(u, other.u, t), lerp(v, other.v, t), lerp(red, other.red, t),
					lerp(green, other.green, t), lerp(blue, other.blue, t), lerp(alpha, other.alpha, t));
		}
	}

	/** Positive Z faces the camera. Rotate the mesh first so the cut always follows the view. */
	public static Vertex rotate(Vertex vertex, float yaw, float pitch) {
		double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
		float x = (float) (vertex.x * cy + vertex.z * sy);
		float z = (float) (-vertex.x * sy + vertex.z * cy);
		return new Vertex(x, (float) (vertex.y * cp - z * sp), (float) (vertex.y * sp + z * cp),
				vertex.u, vertex.v, vertex.red, vertex.green, vertex.blue, vertex.alpha);
	}

	/** Sutherland-Hodgman clipping of a convex face, including interpolated texture coordinates. */
	public static List<Vertex> clip(List<Vertex> face, float plane) {
		if (face.isEmpty()) return List.of();
		List<Vertex> result = new ArrayList<>(face.size() + 1);
		Vertex previous = face.getLast();
		boolean previousInside = previous.z <= plane;
		for (Vertex current : face) {
			boolean inside = current.z <= plane;
			if (inside != previousInside) {
				float t = (plane - previous.z) / (current.z - previous.z);
				result.add(previous.interpolate(current, t));
			}
			if (inside) result.add(current);
			previous = current;
			previousInside = inside;
		}
		return result.size() < 3 ? List.of() : result;
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}
}
