#!/bin/sh
# HOW TO RUN:
# deploynhl.sh [versionNumber]
echo "Deploying nhlbot-$1.jar from '/ftp/files'"
echo "Stopping Service..."
/bin/systemctl stop nhlbot
echo "Moving .jar..."
mv /ftp/files/nhlbot-$1.jar /discord-bots/nhlbot/builds/nhlbot-$1.jar
echo "Linking build .jar..."
ln -sf /discord-bots/nhlbot/builds/nhlbot-$1.jar /discord-bots/nhlbot/builds/current.jar
echo "Starting Service..."
/bin/systemctl start nhlbot
echo "Done!"