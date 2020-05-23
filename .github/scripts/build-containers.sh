#!/usr/bin/env bash

set -euo pipefail
IFS=$'\n\t'

usage() {
    echo "==# Expected docker image tag as 1st arg"
    exit 1
}

set -v

git fetch origin master
short_sha=$( git rev-parse --short HEAD )
docker_image_tag=${1-${short_sha}}

[[ -z "${docker_image_tag}" ]] && usage

for container in $( ls docker ); do
    container_directory="docker/${container}"

    if [[ -d "${container_directory}" ]] && ! git diff --quiet $( git merge-base origin/master HEAD ) HEAD -- ${container_directory}; then
        cd ${container_directory}
        docker_image_full_path=$(echo docker.pkg.github.com/${GITHUB_REPOSITORY}/${container}:${docker_image_tag} | tr '[:upper:]' '[:lower:]')
        docker login -u publisher -p ${DOCKER_TOKEN} docker.pkg.github.com
        eval "docker build -t ${docker_image_full_path} ."
        docker push ${docker_image_full_path}
        cd -
     else
        echo "==> Skipping container ${container}"
    fi
done
