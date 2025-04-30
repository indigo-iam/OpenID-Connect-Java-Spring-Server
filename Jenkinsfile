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
        configFileProvider([configFile(fileId: 'cnafsd-maven-settings', variable: 'MAVEN_SETTINGS')]) {
          withCredentials([usernamePassword(credentialsId: 'jenkins-nexus', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
            sh 'mvn -Dserver.username=$USERNAME -Dserver.password=$PASSWORD -s $MAVEN_SETTINGS clean deploy'
          }
        }
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
