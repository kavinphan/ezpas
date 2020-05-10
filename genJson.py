PIPES = [
    "white_stained_pipe",
    "orange_stained_pipe",
    "magenta_stained_pipe",
    "light_blue_stained_pipe",
    "yellow_stained_pipe",
    "lime_stained_pipe",
    "pink_stained_pipe",
    "gray_stained_pipe",
    "light_gray_stained_pipe",
    "cyan_stained_pipe",
    "purple_stained_pipe",
    "blue_stained_pipe",
    "brown_stained_pipe",
    "green_stained_pipe",
    "red_stained_pipe",
    "black_stained_pipe",
    "filtered_pipe_whitelist",
    "filtered_pipe_blacklist",
    "pipe"
]


blockstatesDir = "src/main/resources/assets/ezpas/blockstates/"
modelsDir = "src/main/resources/assets/ezpas/models/"
blockModelsDir = modelsDir + "block/pipe/"
itemModelsDir = modelsDir + "item/"


def write_json(path, json):
    f = open(path, "w")
    f.write(json)
    f.close()


def bs_json(name):
    return """{{
  \"multipart\": [
    {{
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_center\" }}
    }},
    {{
      \"when\": {{ \"north\": true }},
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_side\" }}
    }},
    {{
      \"when\": {{ \"east\": true }},
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_side\", \"y\": 90 }}
    }},
    {{
      \"when\": {{ \"south\": true }},
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_side\", \"y\": 180 }}
    }},
    {{
      \"when\": {{ \"west\": true }},
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_side\", \"y\": 270 }}
    }},
    {{
      \"when\": {{ \"up\": true }},
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_side\", \"x\": 270}}
    }},
    {{
      \"when\": {{ \"down\": true }},
      \"apply\": {{ \"model\": \"ezpas:block/pipe/{}_side\", \"x\": 90 }}
    }}
  ]
}}""".format(name, name, name, name, name, name, name)


def pipe_center_json(name):
    return """{{
  "parent": "ezpas:block/template/pipe_center",
  "textures": {{
    "pipe": "ezpas:block/{}"
  }}
}}""".format(name)


def pipe_side_json(name):
    return """{{
  "parent": "ezpas:block/template/pipe_side",
  "textures": {{
    "pipe": "ezpas:block/{}"
  }}
}}""".format(name)


def item_json(name):
    return """{{
  "parent": "ezpas:block/pipe/{}_center"
}}""".format(name)


def write_pipe(name):
    write_json("{}{}.json".format(blockstatesDir, name), bs_json(name))
    write_json("{}{}_center.json".format(blockModelsDir, name), pipe_center_json(name))
    write_json("{}{}_side.json".format(blockModelsDir, name), pipe_side_json(name))
    write_json("{}{}.json".format(itemModelsDir, name), item_json(name))


for pipe in PIPES:
    write_pipe(pipe)