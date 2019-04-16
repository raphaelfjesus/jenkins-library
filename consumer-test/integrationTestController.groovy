import ITUtils

WORKSPACES_ROOT = 'workspaces'
TEST_CASES_DIR = 'testCases'

/*
In case the build is performed for a pull request TRAVIS_COMMIT is a merge
commit between the base branch and the PR branch HEAD. That commit is actually built.
But for notifying about a build status we need the commit which is currently
the HEAD of the PR branch.

In case the build is performed for a simple branch (not associated with a PR)
In this case there is no merge commit between any base branch and HEAD of a PR branch.
The commit which we need for notifying about a build status is in this case simply
TRAVIS_COMMIT itself.
*/
ITUtils.commitHash = System.getenv('TRAVIS_PULL_REQUEST_SHA') ?: System.getenv('TRAVIS_COMMIT')

ITUtils.notifyGithub("pending", "Integration tests are in progress.")

ITUtils.newEmptyDir(WORKSPACES_ROOT)
ITUtils.workspacesRootDir = WORKSPACES_ROOT
ITUtils.libraryVersionUnderTest = "git log --format=%H -n 1".execute().text.trim()
ITUtils.repositoryUnderTest = System.getenv('TRAVIS_REPO_SLUG') ?: 'SAP/jenkins-library'

//This auxiliary thread is needed in order to produce some output while the
//test are running. Otherwise the job will be canceled after 10 minutes without output.
def auxiliaryThread = Thread.start {
    sleep(10000)
    println "[INFO] Integration tests still running."
}

def testCaseThreads = listTestCaseThreads()
testCaseThreads.each { it ->
    it.start()
    it.join()
}

auxiliaryThread.join()

ITUtils.notifyGithub("success", "The integration tests succeeded.")


def listTestCaseThreads() {
    //Each dir that includes a yml file is a test case
    def testCases = ITUtils.listYamlInDirRecursive(TEST_CASES_DIR)
    def threads = []
    testCases.each { file ->
        threads << new TestRunnerThread(file.toString())
    }
    return threads
}
