with open('app/src/main/res/layout/custom_player_view.xml', 'r') as file:
    data = file.read()

data = data.replace('app:show_buffering="always"', 'app:show_buffering="when_playing"')

with open('app/src/main/res/layout/custom_player_view.xml', 'w') as file:
    file.write(data)
