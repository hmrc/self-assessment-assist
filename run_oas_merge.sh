#!/bin/bash

if [ "$#" -lt 2 ]
then
    printf "\n Usage: ./run_oas_merge.sh <<api_version_no>> <<scala_version_no>> [-v]. Example ./run_oas_merge.sh 2.0 2.12 -v"
    exit 1
fi

if ! [[ $1 == "1.0" || $1 == "2.0" ]]
then
    printf "\n Only API version no supported for now are 1.0 or 2.0  \n"
    exit 1
fi

if ! [[ $2 == "2.12" || $2 == "2.13" ]]
then
    printf "\n Only Scala version no supported for now are 2.12 or 2.13 \n"
    exit 1
fi


if ! [ -x "$(command -v npm)" ]
then
    printf "\n npm is not installed, install node/npm. See the readme.md \n"
    exit 1
fi

# handle jenkins to change to tmp directory as workaround
# to handle speccy issue with spaces in path
running_on_jenkins=false
current_dir=`pwd`
tmp_dir="${current_dir// /-}-tmp"

if [[ "$current_dir" == *"MTD API"* ]]
then
	printf "\n Running inside jenkins, so doing some workaround ..."
	running_on_jenkins=true
fi

target_path="target/scala-$2/classes/public/api/conf/$1"
target_file_path="$target_path/application.yaml"
if [[ -e "$target_file_path" &&  "$running_on_jenkins" = true ]]
then
    printf "\n OAS merged file alreay exists, so not running speccy merge \n"
else
	if [ "$running_on_jenkins" = true ]
	then
		mkdir -p "$tmp_dir"
		cd "$tmp_dir"
		cp -r "$current_dir/." .
	fi

	if ! [ -e node_modules/.bin/speccy ]
	then
	    printf "\n speccy is not installed, installing speccy\n"
	    npm install
	fi

	printf "\n Running speccy to merge modular OAS spec files.... \n"
	mkdir -p "$target_path"

	if [ "$3" == "-v" ]
	then
		API_VERSION=$1 SCALA_VERSION=$2  npm run oasMergeVerbose
	else
		API_VERSION=$1 SCALA_VERSION=$2 npm run oasMerge
	fi


	error=false
	if ! [ $? -eq 0 ]
	then
		printf "\n Error, OAS spec merge failed, run verbose using -v option : 'sbt oasMergeVerbose' or 'VERSION=$1 SCALA_VERSION=$2 npm run oasMergeVerbose' \n"
		error=true
	fi

	printf '\n Checking for any nested yaml/json files which are not merged... \n'
	if [[ "$error" = false && `grep '\.yaml\|\.json' "$target_file_path" -c` -gt 0 ]]
	then
	 	printf "\n Error, found some nested Yaml/Json files which are not merged. \n"
	    error=true
	fi

	if [ "$running_on_jenkins" = true ]
	then
		printf "\n cleanup and change back to original jenkins directory"
		cd "$current_dir"
		mkdir -p "${current_dir}/$target_path/"
		cp "${tmp_dir}/$target_file_path" "${current_dir}/$target_file_path"
		cd ../../..
		rm -rf MTD-API
	fi

	if [ "$error" = true ]
	then
		exit 1
	fi

fi

