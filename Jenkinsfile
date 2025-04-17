#!/usr/bin/env groovy

pipeline {

  agent { 
    docker {
      label 'docker'
      image 'maven:3.9.6-eclipse-temurin-17'
      args '--privileged'
      reuseNode true
    }
  }

  options {
    timeout(time: 1, unit: 'HOURS')
    buildDiscarder(logRotator(numToKeepStr: '5'))
  }

  triggers { cron('@daily') }

  stages {

    stage('deploy') {
      steps {
        sh "mvn -U -B clean package deploy"
      }
    }
    
    stage('result'){
      steps {
        script { 
          currentBuild.result = 'SUCCESS' 
        }
      }
    }
  }
}
