param(
    [Parameter(Mandatory=$true)]
    [string]$VmName
)

$uerName = "admin" 
$userPwd = ConvertTo-SecureString "admin" -AsPlainText -Force  
$cred = New-Object System.Management.Automation.PSCredential ($uerName, $userPwd) 
$session = New-PSSession -VMName $VmName -Credential $cred 
Copy-Item -ToSession $session -Path "C:\Windows\System32\DriverStore\FileRepository\u0386690.inf_amd64_89cce67a8dae96c6\*" -Destination "C:\Windows\System32\DriverStore\FileRepository\u0386690.inf_amd64_89cce67a8dae96c6\" 
Copy-Item -ToSession $session -Path "C:\Windows\System32\amdihk64.dll" -Destination "C:\Windows\System32\" 
Remove-PSSession -Session $session
