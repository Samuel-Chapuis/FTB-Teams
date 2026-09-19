package dev.ftb.mods.ftbteams.client.gui;

import java.util.List;

import static dev.ftb.mods.ftbteams.client.gui.PreviewGeometry.*;

/** Tests actual polygon slicing, not just block-center visibility. No rendering context required. */
public final class PreviewGeometryTest {
	private static int assertions;

	public static void main(String[] args) {
		List<Vertex> quad = List.of(v(0, 0, 0, 0, 0), v(1, 0, 1, 1, 0), v(1, 1, 1, 1, 1), v(0, 1, 0, 0, 1));
		expect(clip(quad, 2).equals(quad), "Entirely behind the cut: unchanged");
		expect(clip(quad, -1).isEmpty(), "Entirely in front of the cut: hidden");
		List<Vertex> half = clip(quad, 0.5F);
		expect(half.size() == 4, "A sliced wall is still a textured polygon");
		for (Vertex vertex : half) {
			expect(vertex.z() <= 0.5F && Float.isFinite(vertex.x()), "No geometry in front of the cut / no NaN");
			if (close(vertex.z(), 0.5F)) expect(close(vertex.u(), 0.5F) && close(vertex.x(), 0.5F), "UV and position interpolate at the same intersection");
		}
		List<Vertex> diagonal = List.of(v(0, 0, 0, 0, 0), v(1, 0, 1, 1, 0), v(1, 1, 2, 1, 1), v(0, 1, 1, 0, 1));
		expect(clip(diagonal, 1.5F).size() == 5, "Corner cut produces a pentagon, not a discarded whole block");
		List<Vertex> coplanar = List.of(v(0, 0, 0, 0, 0), v(1, 0, 0, 1, 0), v(1, 1, 0, 1, 1), v(0, 1, 0, 0, 1));
		expect(clip(coplanar, 0).equals(coplanar), "Coplanar surfaces survive without division by zero");
		expect(clip(List.of(), 0).isEmpty(), "Empty mesh");
		Vertex right = rotate(v(1, 0, 0, 0.2F, 0.7F), (float) (Math.PI / 2), 0);
		expect(close(right.x(), 0) && close(right.z(), -1), "A quarter turn moves the cut from front to side");
		expect(close(right.u(), 0.2F) && close(right.v(), 0.7F), "Rotation leaves texture coordinates unchanged");
		Vertex roof = rotate(v(0, 1, 0, 0, 0), 0, (float) (Math.PI / 2));
		expect(close(roof.y(), 0) && close(roof.z(), 1), "Tilting the view brings the roof in front of the cut");
		Vertex a = new Vertex(0, 0, 0, 0, 0, 0, 0, 0, 0);
		Vertex b = new Vertex(1, 1, 1, 1, 1, 1, 1, 1, 1);
		Vertex mid = a.interpolate(b, 0.5F);
		expect(close(mid.red(), 0.5F) && close(mid.alpha(), 0.5F), "Clipping preserves color/alpha gradients");
		for (int degrees = -180; degrees <= 180; degrees += 15) {
			float yaw = (float) Math.toRadians(degrees);
			var face = quad.stream().map(p -> rotate(p, yaw, 0.5F)).toList();
			for (Vertex vertex : clip(face, 0.1F)) expect(vertex.z() <= 0.10001F, "Cut follows every camera angle");
		}
		System.out.println("Preview geometry regression tests passed (" + assertions + " assertions).");
	}

	private static Vertex v(float x, float y, float z, float u, float v) {
		return new Vertex(x, y, z, u, v, 1, 1, 1, 1);
	}

	private static boolean close(float a, float b) {
		return Math.abs(a - b) < 0.00001F;
	}

	private static void expect(boolean condition, String message) {
		assertions++;
		if (!condition) throw new AssertionError(message);
	}
}
