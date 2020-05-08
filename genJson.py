BLOCKS = [
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
    "black_stained_pipe"
]


ITEMS = [
]


TOOLS = [
]


BOWS = [
]


modelsDir = "src/main/resources/assets/ezpas/models/"
itemModelsDir = modelsDir + "item/"
blockModelsDir = modelsDir + "block/"
blockstatesDir = "src/main/resources/assets/ezpas/blockstates/"


def write_json(path, json):
    f = open(path, "w")
    f.write(json)
    f.close()


def block_json(name):
    return """{{
    \"parent\": \"block/cube_all\",
    \"textures\": {{
        \"all\": \"ezpas:block/{}\"
    }}
}}""".format(name)


def item_block_json(name):
    return """{{
  \"parent\": \"ezpas:block/{}\"
}}""".format(name)


def blockstate_json(name):
    return """{{
    \"variants\": {{
        \"\": {{ \"model\": \"ezpas:block/{}\" }}
    }}
}}""".format(name)


def item_json(name):
    return """{{
  \"parent\": \"item/generated\",
  \"textures\": {{
    \"layer0\": \"ezpas:item/{}\"
  }}
}}""".format(name)


def tool_json(name):
    return """{{
  \"parent\": \"minecraft:item/handheld\",
  \"textures\": {{
    \"layer0\": \"ezpas:item/{}\"
  }}
}}""".format(name)


def bow_json(name):
    return """{{
  \"parent\": \"item/generated\",
  \"textures\": {{
    \"layer0\": \"ezpas:item/{}\"
  }},
  \"display\": {{
    \"thirdperson_righthand\": {{
      \"rotation\": [ -80, 260, -40 ],
      \"translation\": [ -1, -2, 2.5 ],
      \"scale\": [ 0.9, 0.9, 0.9 ]
    }},
    \"thirdperson_lefthand\": {{
      \"rotation\": [ -80, -280, 40 ],
      \"translation\": [ -1, -2, 2.5 ],
      \"scale\": [ 0.9, 0.9, 0.9 ]
    }},
    \"firstperson_righthand\": {{
      \"rotation\": [ 0, -90, 25 ],
      \"translation\": [ 1.13, 3.2, 1.13],
      \"scale\": [ 0.68, 0.68, 0.68 ]
    }},
    \"firstperson_lefthand\": {{
      \"rotation\": [ 0, 90, -25 ],
      \"translation\": [ 1.13, 3.2, 1.13],
      \"scale\": [ 0.68, 0.68, 0.68 ]
    }}
  }},
  \"overrides\": [
    {{
      \"predicate\": {{
        \"pulling\": 1
      }},
      \"model\": \"ezpas:item/{}_pulling_0\"
    }},
    {{
      \"predicate\": {{
        \"pulling\": 1,
        \"pull\": 0.65
      }},
      \"model\": \"ezpas:item/{}_pulling_1\"
    }},
    {{
      \"predicate\": {{
        \"pulling\": 1,
        \"pull\": 0.9
      }},
      \"model\": \"ezpas:item/{}_pulling_2\"
    }}
  ]
}}""".format(name, name, name, name)


def write_block(name):
    write_json("{}{}.json".format(blockModelsDir, name), block_json(name))
    write_json("{}{}.json".format(itemModelsDir, name), item_block_json(name))
    write_json("{}{}.json".format(blockstatesDir, name), blockstate_json(name))


def write_item(name):
    write_json("{}{}.json".format(itemModelsDir, name), item_json(name))


def write_tool(name):
    write_json("{}{}.json".format(itemModelsDir, name), tool_json(name))


def write_bow(name):
    write_json("{}{}.json".format(itemModelsDir, name), bow_json(name))


for block in BLOCKS:
    write_block(block)

for item in ITEMS:
    write_item(item)

for tool in TOOLS:
    write_tool(tool)

for bow in BOWS:
    write_bow(bow)