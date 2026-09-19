"""Convert the original Blockbench exports to Minecraft 1.21.1 models.

Run from any directory with Python 3. Sources are preserved; lowercase runtime
models are regenerated. Supports the zero / -90 degree Y rotations in these exports.
"""
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "common/src/main/resources/assets/ftbteams"
MODELS = ASSETS / "models/block"
COLORS = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")
DEFAULT_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.45] * 3},
    "ground": {"translation": [0, 3, 0], "scale": [0.3] * 3},
    "fixed": {"rotation": [0, 90, 0], "scale": [0.45] * 3},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3] * 3},
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.4] * 3},
}


def read(name):
    return json.loads((MODELS / name).read_text(encoding="utf-8"))


def write(path, model):
    path.write_text(json.dumps(model, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def rotate(point, origin):
    # Bake -90 degrees around Y; no unsupported element rotations remain.
    x, y, z = point
    ox, _, oz = origin
    return [ox - z + oz, y, oz + x - ox]


def convert(source):
    textures = {}
    for key, value in source["textures"].items():
        value = value.removeprefix("minecraft:").removeprefix("block/")
        if value == "quartz_pillar_side":
            value = "quartz_pillar"
        textures[key] = "minecraft:block/" + value
    textures["particle"] = textures["3"]
    elements = []
    for original in source["elements"]:
        if all(face["texture"] == "#missing" for face in original["faces"].values()):
            continue  # Untextured reference cube outside the bed.
        element = copy.deepcopy(original)
        element.pop("color", None)
        rotation = element.pop("rotation", {})
        angle = rotation.get("angle", rotation.get("y", 0))
        if rotation.get("axis", "y") != "y" or rotation.get("x", 0) or rotation.get("z", 0) or angle not in (0, -90):
            raise ValueError("Unsupported new rotation; update the converter: " + str(rotation))
        if angle == -90:
            ends = [rotate(element[key], rotation["origin"]) for key in ("from", "to")]
            element["from"] = [min(a, b) for a, b in zip(*ends)]
            element["to"] = [max(a, b) for a, b in zip(*ends)]
            directions = {"north": "east", "east": "south", "south": "west", "west": "north", "up": "up", "down": "down"}
            element["faces"] = {directions[side]: face for side, face in element["faces"].items()}
            for side, extra in (("up", 90), ("down", 270)):
                if side in element["faces"]:
                    face = element["faces"][side]
                    face["rotation"] = (face.get("rotation", 0) + extra) % 360
        for face in element["faces"].values():
            # Keep the UV rectangle's size and mirroring, but move it inside its
            # sprite. Out-of-range UVs would otherwise sample neighboring atlas textures.
            uv = face["uv"]
            for a, b in ((0, 2), (1, 3)):
                low, high = sorted((uv[a], uv[b]))
                if high - low > 16:
                    raise ValueError("UV rectangle wider than its sprite")
                shift = -low if low < 0 else 16 - high if high > 16 else 0
                uv[a] += shift
                uv[b] += shift
            assert face["texture"][1:] in textures, face["texture"]
        elements.append(element)
    result = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if "display" in source:
        result["display"] = copy.deepcopy(source["display"])
    return result


def main():
    full = convert(read("PopBed.json"))
    foot = convert(read("PopBedBot.json"))
    head = convert(read("PopBedTop.json"))
    # Bot currently exports the entire bed. Retain the local foot half only.
    foot["elements"] = [e for e in foot["elements"] if e["from"][2] >= 0 and e["to"][2] <= 16]
    for part in (foot, head):
        assert part["elements"]
        assert all(0 <= n <= 16 for e in part["elements"] for key in ("from", "to") for n in e[key])
    if "display" not in full:
        # Only center exports without authored display transforms; preserve supplied HUD settings.
        for element in full["elements"]:
            element["from"][2] -= 8
            element["to"][2] -= 8
        full["display"] = DEFAULT_DISPLAY
    write(MODELS / "pop_bed.json", full)
    write(MODELS / "pop_bed_foot.json", foot)
    write(MODELS / "pop_bed_head.json", head)
    write(ASSETS / "models/item/pop_bed.json", {"parent": "ftbteams:block/pop_bed"})
    generate_colors()
    print(f"Converted Pop Bed: {len(full['elements'])} item, {len(foot['elements'])} foot, {len(head['elements'])} head elements.")


def generate_colors():
    resources = ASSETS.parents[1]
    ids = []
    for color in COLORS:
        # Cyan retains the original ID so existing worlds keep their beds and contents.
        name = "pop_bed" if color == "cyan" else f"{color}_pop_bed"
        ids.append("ftbteams:" + name)
        if color != "cyan":
            for suffix in ("", "_head", "_foot"):
                write(MODELS / f"{name}{suffix}.json", {
                    "parent": f"ftbteams:block/pop_bed{suffix}",
                    "textures": {"1": f"minecraft:block/{color}_shulker_box"},
                })
            write(ASSETS / f"models/item/{name}.json", {"parent": f"ftbteams:block/{name}"})
        # Exported models point south (pillow at +Z); both halves rotate together.
        variants = {
            f"facing={direction},part={part}": {"model": f"ftbteams:block/{name}_{part}", "y": angle}
            for direction, angle in (("south", 0), ("west", 90), ("north", 180), ("east", 270))
            for part in ("head", "foot")
        }
        write(ASSETS / f"blockstates/{name}.json", {"variants": variants})
        write(resources / f"data/ftbteams/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:block_state_property", "block": f"ftbteams:{name}", "properties": {"part": "head"}},
            ], "entries": [{"type": "minecraft:item", "name": f"ftbteams:{name}"}]}],
        })
    write(resources / "data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": ids})


if __name__ == "__main__":
    main()
