# NOTE:
# The execution of PowerShell scripts is generally disabled by default. Windows 10
# users who have enabled "developer mode" on their machine should be able to run
# PowerShell scripts with no restrictions.
#
# If enabling developer mode is undesirable in a particular case, one can start a PS
# session with an unrestricted execution policy by running the following command:
#
# powershell.exe -ExecutionPolicy Unrestricted
#
# This command will open a new PowerShell session with unrestricted execution policy,
# in which this script can be executed. The execution policy only applies to that
# specific PS session and has no effect on execution policy settings in general.


[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$scriptDir = Split-Path -Path $MyInvocation.MyCommand.Definition -Parent
echo "`n$scriptDir `nDownloading dependencies ..."

# create an output directory if not one already
if (!(Test-Path -Path $scriptDir\contrib)) {
    echo "Making contrib directory ..."
    mkdir $scriptDir\contrib
}

# download and unzip objenesis if not exists
if (!(Test-Path "${scriptDir}\contrib\objenesis.jar")) {
    if (!(Test-Path "${scriptDir}\contrib\objenesis.zip")) {
        Invoke-WebRequest `
        -Uri "https://bintray.com/artifact/download/easymock/distributions/objenesis-2.6-bin.zip" `
        -OutFile "${scriptDir}\contrib\objenesis.zip"
    }
    Expand-Archive "${scriptDir}\contrib\objenesis.zip" -DestinationPath "${scriptDir}\contrib\" -Force
}

# download and unzip javassist if not exists
if (!(Test-Path "${scriptDir}\contrib\javassist.jar")) {
    if (!(Test-Path "${scriptDir}\contrib\javassist.zip")) {
        Invoke-WebRequest `
        -Uri "https://github.com/jboss-javassist/javassist/zipball/master" `
        -OutFile "${scriptDir}\contrib\javassist.zip"
    }
    Expand-Archive "${scriptDir}\contrib\javassist.zip" -DestinationPath "${scriptDir}\contrib\" -Force
}

# download and unzip asm if not exists
if (!(Test-Path "${scriptDir}\contrib\asm-all.jar")) {
    if (!(Test-Path "${scriptDir}\contrib\asm-all.zip")) {
        Invoke-WebRequest `
        -Uri "http://central.maven.org/maven2/org/ow2/asm/asm-all/6.0_BETA/asm-all-6.0_BETA.jar" `
        -OutFile "${scriptDir}\contrib\asm-all.jar"
    }
}

# download and unzip retrolambda if not exists
if (!(Test-Path "${scriptDir}\contrib\retrolambda.jar")) {
    Invoke-WebRequest `
    -Uri "https://oss.sonatype.org/content/groups/public/net/orfjackal/retrolambda/retrolambda/2.5.4/retrolambda-2.5.4.jar" `
    -OutFile "${scriptDir}\contrib\retrolambda.jar"
}

echo "... downloaded."