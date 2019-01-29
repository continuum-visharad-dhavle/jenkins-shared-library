#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object    
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    
    //def slaveTemplate = libraryResource 'org/continuum/shared-lib/goTemplate.yaml'
    def label = "go-${UUID.randomUUID().toString()}"
    podTemplate(label: label, yaml: """
apiVersion: v1
kind: Pod
metadata:
  labels:
    some-label: some-label-value
spec:
  containers:
  - name: maven
    image: maven:3.3.9-jdk-8-alpine
    command: ['cat']
    tty: true
"""
)   {
        body() 
        node(label){
            container('maven') {

                stage('checkout')
                {
                    checkout scm
                }

                stage('build+test')
                {
                    sh 'echo building application'
                    sh 'echo testing application'
                }

                stage('sonar')
                {
                    sh 'echo executing sonar analysis'
                }

                stage('coverage')
                {
                    sh 'echo performing code coverage'
                }
            }

            container('maven') {

                stage('docker build')
                {
                    sh 'echo building docker image'
                }

                stage('docker publish')
                {
                    sh 'echo publishing docker image'
                }           

            }

            stage('parallel tasks')
            {
                parallel a:
                {
                    container('maven') 
                    {
                        stage('upload artifacts')
                        {
                            sh 'echo configure artifactory'
                        }
                    }
                },b:                 
                {
                    container('maven') 
                    {
                        stage('promote build')
                        {
                            sh 'echo promoting build'
                        }
                    }
                }
            }
        }

        input 'Do you approve int deployment?'
        cd
        {
           env = 'int'
        }

        input 'Do you approve qa deployment?'
        node(label){
            container('maven') {
                stage('deploy-qa')
                sh 'echo Execution after approval'
            }
        }

        input 'Do you approve stage deployment?'
        node(label){
            container('maven') {
                stage('deploy-stage')
                sh 'echo Execution after approval'
            }
        }

        input 'Do you approve prod deployment?'
        node(label){
            container('maven') {
                stage('deploy-prod')
                sh 'echo Execution after approval'
            }
        }
    }
}