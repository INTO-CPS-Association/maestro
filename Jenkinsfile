node {
	try
  {

		
		stage ('Checkout'){
			checkout scm
		}

		// Mark the code build
		stage ('Build'){

			withMaven(mavenLocalRepo: '.repository', mavenSettingsFilePath: "${env.MVN_SETTINGS_PATH}") {

				lock('FMU-SHM') {
					// Run the maven build
					sh "mvn clean install -U -Phtlatex -Dsimulation.profile.executiontime=true"
				}
				
				step([$class: 'ArtifactArchiver', artifacts: '**/target/*.jar', fingerprint: true])
				step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
				step([$class: 'JacocoPublisher'])
				step([$class: 'TasksPublisher', canComputeNew: false, defaultEncoding: '', excludePattern: '', healthy: '', high: 'FIXME', ignoreCase: true, low: '', normal: 'TODO', pattern: '', unHealthy: ''])
			}
		}

		stage ('Publish Artifactory'){

			if (env.BRANCH_NAME == 'development') {
			
				def server = Artifactory.server "-844406945@1404457436085"
				def buildInfo = Artifactory.newBuildInfo()
				buildInfo.env.capture = true
				//buildInfo.env.filter.addExclude("org/destecs/ide/**")
				def rtMaven = Artifactory.newMavenBuild()
				rtMaven.tool = "Maven 3.1.1" // Tool name from Jenkins configuration
				rtMaven.opts = "-Xmx1024m -XX:MaxPermSize=256M"
				rtMaven.deployer releaseRepo:'into-cps', snapshotRepo:'into-cps', server: server
				
				rtMaven.run pom: 'pom.xml', goals: 'install', buildInfo: buildInfo

				//get rid of old snapshots only keep then for a short amount of time
				buildInfo.retention maxBuilds: 5, maxDays: 7, deleteBuildArtifacts: true
		
				// Publish build info.
				server.publishBuildInfo buildInfo
			}
		}
	

	} catch (any) {
		currentBuild.result = 'FAILURE'
		throw any //rethrow exception to prevent the build from proceeding
	} finally {
  
		stage('Reporting'){

			// Notify on build failure using the Email-ext plugin
			emailext(body: '${DEFAULT_CONTENT}', mimeType: 'text/html',
							 replyTo: '$DEFAULT_REPLYTO', subject: '${DEFAULT_SUBJECT}',
							 to: emailextrecipients([[$class: 'CulpritsRecipientProvider'],
																			 [$class: 'RequesterRecipientProvider']]))
		}}
}

