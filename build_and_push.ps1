param (
    [Parameter(Mandatory=$true, HelpMessage="Enter your Docker Hub username")]
    [string]$DockerHubUsername
)

$services = @(
    @{ Name = "user-service"; Path = "UserService" },
    @{ Name = "activity-service"; Path = "ActivityService" },
    @{ Name = "ai-service"; Path = "aiService" },
    @{ Name = "config-service"; Path = "configService" },
    @{ Name = "eureka-server"; Path = "eureka" },
    @{ Name = "gateway-service"; Path = "gateway" },
    @{ Name = "frontend"; Path = "frontend" },
    @{ Name = "keycloak"; Path = "keycloak" }
)

Write-Host "Building and pushing Docker images for $($services.Count) services..." -ForegroundColor Cyan

foreach ($service in $services) {
    $imageName = "$($DockerHubUsername)/$($service.Name):latest"
    $servicePath = $service.Path
    
    Write-Host "`n=================================================" -ForegroundColor Yellow
    Write-Host "Building image: $imageName from $servicePath" -ForegroundColor Yellow
    Write-Host "=================================================`n" -ForegroundColor Yellow
    
    # Build the image
    docker build -t $imageName "./$servicePath"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to build $imageName. Exiting." -ForegroundColor Red
        exit $LASTEXITCODE
    }
    
    Write-Host "Pushing image: $imageName..." -ForegroundColor Green
    # Push the image
    docker push $imageName
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to push $imageName. Exiting." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

Write-Host "`nAll images have been successfully built and pushed to Docker Hub!" -ForegroundColor Green
