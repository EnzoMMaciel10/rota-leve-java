[CmdletBinding()]
param(
    [switch]$ComIA,
    [ValidateSet('ollama', 'openai')]
    [string]$ProvedorIA = 'ollama'
)

$ErrorActionPreference = 'Stop'
$previousLocation = Get-Location
$projectDirectory = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $projectDirectory 'compose.yaml'
$jarFile = Join-Path $projectDirectory 'target\rota-leve.jar'
$wrapperFile = Join-Path $projectDirectory 'mvnw.cmd'
$changedVariables = @(
    'SPRING_PROFILES_ACTIVE', 'RABBIT_ENABLED', 'RABBIT_HOST', 'RABBIT_PORT',
    'RABBIT_USER', 'RABBIT_PASSWORD', 'AI_ENABLED', 'AI_CHAT_MODEL', 'OPENAI_API_KEY',
    'OLLAMA_BASE_URL', 'OLLAMA_MODEL'
)
$previousEnvironment = @{}
foreach ($variableName in $changedVariables) {
    $previousEnvironment[$variableName] = [Environment]::GetEnvironmentVariable($variableName, 'Process')
}
$scriptExitCode = 0

function Invoke-CapturedCommand {
    param([string]$FilePath, [string[]]$Arguments, [int]$TimeoutSeconds = 5)
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    # Argumentos internos conhecidos; nunca recebem a chave da OpenAI.
    $startInfo.Arguments = (($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' }) -join ' ')
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    try {
        $null = $process.Start()
        $outputTask = $process.StandardOutput.ReadToEndAsync()
        $errorTask = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            try { $process.Kill() } catch { }
            return [pscustomobject]@{ ExitCode = -1; Output = ''; ErrorOutput = '' }
        }
        return [pscustomobject]@{ ExitCode = $process.ExitCode; Output = $outputTask.Result; ErrorOutput = $errorTask.Result }
    } finally {
        $process.Dispose()
    }
}

function Test-LocalPort {
    param([int]$Port)
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne(500)) { return $false }
        $client.EndConnect($connection)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Test-DockerEngine {
    try {
        $result = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('info', '--format', '{{.ServerVersion}}')
        return $result.ExitCode -eq 0
    } catch {
        return $false
    }
}

function Get-RabbitContainerId {
    try {
        $result = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('compose', '-p', 'rotaleve', '-f', $script:composeFile, 'ps', '-a', '-q', 'rabbitmq')
        if ($result.ExitCode -ne 0) { return $null }
        return (($result.Output -split '\r?\n' | Where-Object { $_.Trim() } | Select-Object -First 1))
    } catch {
        return $null
    }
}

function Get-OllamaContainerId {
    try {
        $result = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('compose', '-p', 'rotaleve', '-f', $script:composeFile, '--profile', 'ia', 'ps', '-a', '-q', 'ollama')
        if ($result.ExitCode -ne 0) { return $null }
        return (($result.Output -split '\r?\n' | Where-Object { $_.Trim() } | Select-Object -First 1))
    } catch {
        return $null
    }
}

function Test-RabbitHealthy {
    param([string]$ContainerId)
    if ([string]::IsNullOrWhiteSpace($ContainerId)) { return $false }
    try {
        $result = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('inspect', '--format', '{{if .State.Health}}{{.State.Health.Status}}{{end}}', $ContainerId)
        return $result.ExitCode -eq 0 -and $result.Output.Trim() -eq 'healthy'
    } catch {
        return $false
    }
}

try {
    Set-Location -LiteralPath $projectDirectory
    if (-not (Test-Path -LiteralPath $composeFile -PathType Leaf)) {
        throw 'compose.yaml nao encontrado. Mantenha este script dentro da pasta scripts do projeto.'
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaExecutable = Join-Path $env:JAVA_HOME 'bin\java.exe'
        $javacExecutable = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    } else {
        $javaCommand = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue
        $javacCommand = Get-Command javac.exe -CommandType Application -ErrorAction SilentlyContinue
        $javaExecutable = if ($null -ne $javaCommand) { $javaCommand.Source } else { '' }
        $javacExecutable = if ($null -ne $javacCommand) { $javacCommand.Source } else { '' }
    }
    if ([string]::IsNullOrWhiteSpace($javaExecutable) -or [string]::IsNullOrWhiteSpace($javacExecutable) -or
        -not (Test-Path -LiteralPath $javaExecutable -PathType Leaf) -or
        -not (Test-Path -LiteralPath $javacExecutable -PathType Leaf)) {
        throw 'JDK nao encontrado. Instale um JDK 17 ou superior e configure JAVA_HOME antes de tentar novamente.'
    }
    $versionResult = Invoke-CapturedCommand -FilePath $javaExecutable -Arguments @('-version')
    $javaVersionOutput = $versionResult.Output + $versionResult.ErrorOutput
    $javaVersionMatch = [regex]::Match($javaVersionOutput, 'version\s+"(?<major>\d+)')
    if ($versionResult.ExitCode -ne 0 -or -not $javaVersionMatch.Success -or [int]$javaVersionMatch.Groups['major'].Value -lt 17) {
        throw 'Este projeto precisa de JDK 17 ou superior. Confira o JDK indicado por JAVA_HOME.'
    }
    $compilerResult = Invoke-CapturedCommand -FilePath $javacExecutable -Arguments @('-version')
    $compilerMatch = [regex]::Match($compilerResult.Output + $compilerResult.ErrorOutput, 'javac\s+(?<major>\d+)')
    if ($compilerResult.ExitCode -ne 0 -or -not $compilerMatch.Success -or [int]$compilerMatch.Groups['major'].Value -lt 17) {
        throw 'O compilador javac precisa ser da versao 17 ou superior. Confira a instalacao do JDK.'
    }

    $dockerCommand = Get-Command docker.exe -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $dockerCommand) {
        throw 'Docker nao encontrado. Instale o Docker Desktop com suporte a containers Linux e tente novamente.'
    }
    $script:dockerExecutable = $dockerCommand.Source
    Write-Host 'Verificando o Docker Desktop...'
    if (-not (Test-DockerEngine)) {
        Write-Host 'Iniciando o Docker Desktop. Aguarde...'
        try {
            $null = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('desktop', 'start', '--detach') -TimeoutSeconds 10
        } catch {
            # Uma versao sem o comando desktop pode ser iniciada manualmente.
        }
        $dockerWait = [System.Diagnostics.Stopwatch]::StartNew()
        $lastDockerMessage = 0
        while (-not (Test-DockerEngine)) {
            if ($dockerWait.Elapsed.TotalSeconds -ge 90) {
                throw 'O Docker nao ficou disponivel. Abra o Docker Desktop, confira se o mecanismo Linux iniciou e tente novamente. Este script nao redefine configuracoes nem apaga dados do Docker.'
            }
            if ($dockerWait.Elapsed.TotalSeconds -ge ($lastDockerMessage + 15)) {
                Write-Host 'Ainda aguardando o Docker...'
                $lastDockerMessage = [int]$dockerWait.Elapsed.TotalSeconds
            }
            Start-Sleep -Seconds 2
        }
    }
    $composeVersion = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('compose', 'version')
    if ($composeVersion.ExitCode -ne 0) { throw 'Docker Compose indisponivel. Confira a instalacao do Docker Desktop.' }

    # Estes valores sao somente os exemplos locais do projeto, nao credenciais de producao.
    if ([string]::IsNullOrWhiteSpace($env:RABBIT_USER)) { $env:RABBIT_USER = 'rotaleve' }
    if ([string]::IsNullOrWhiteSpace($env:RABBIT_PASSWORD)) { $env:RABBIT_PASSWORD = 'rotaleve-dev' }
    $env:RABBIT_ENABLED = 'true'
    $env:RABBIT_HOST = '127.0.0.1'
    $env:RABBIT_PORT = '5672'
    $env:SPRING_PROFILES_ACTIVE = 'demo'
    $env:AI_ENABLED = 'false'
    $env:AI_CHAT_MODEL = 'none'

    $rabbitContainerId = Get-RabbitContainerId
    if (Test-RabbitHealthy $rabbitContainerId) {
        Write-Host 'RabbitMQ do projeto ja esta pronto; reutilizando.'
    } else {
        if ([string]::IsNullOrWhiteSpace($rabbitContainerId) -and
            ((Test-LocalPort 5672) -or (Test-LocalPort 15672))) {
            throw 'As portas 5672 ou 15672 ja estao ocupadas por outro servico. Encerre a execucao anterior antes de iniciar este RabbitMQ.'
        }
        Write-Host 'Preparando o RabbitMQ real...'
        & $script:dockerExecutable compose -p rotaleve -f $composeFile up -d rabbitmq
        if ($LASTEXITCODE -ne 0) {
            throw 'Nao foi possivel iniciar o RabbitMQ. Confira o Docker Desktop e a mensagem acima. Nenhum dado foi apagado.'
        }
        $rabbitWait = [System.Diagnostics.Stopwatch]::StartNew()
        $lastRabbitMessage = 0
        do {
            $rabbitContainerId = Get-RabbitContainerId
            if (Test-RabbitHealthy $rabbitContainerId) { break }
            if ($rabbitWait.Elapsed.TotalSeconds -ge 90) {
                throw 'O RabbitMQ ainda nao passou na verificacao de disponibilidade. Consulte: docker compose -p rotaleve logs rabbitmq. Depois tente novamente.'
            }
            if ($rabbitWait.Elapsed.TotalSeconds -ge ($lastRabbitMessage + 15)) {
                Write-Host 'Aguardando o RabbitMQ ficar pronto...'
                $lastRabbitMessage = [int]$rabbitWait.Elapsed.TotalSeconds
            }
            Start-Sleep -Seconds 2
        } while ($true)
        Write-Host 'RabbitMQ pronto.'
    }

    $apiPort = 8080
    if (-not [string]::IsNullOrWhiteSpace($env:PORT)) {
        if (-not [int]::TryParse($env:PORT, [ref]$apiPort) -or $apiPort -lt 1 -or $apiPort -gt 65535) {
            throw 'A variavel PORT deve conter uma porta entre 1 e 65535.'
        }
    }
    if (Test-LocalPort $apiPort) {
        throw "A porta $apiPort ja esta ocupada. Encerre a API anterior ou defina PORT com outra porta livre."
    }

    if (-not (Test-Path -LiteralPath $jarFile -PathType Leaf)) {
        Write-Host 'Compilando a API pela primeira vez. Pode ser necessario acesso a internet...'
        & $wrapperFile '-DskipTests' 'package'
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $jarFile -PathType Leaf)) {
            throw 'A compilacao nao terminou. Confira a conexao com a internet, o JDK e as mensagens acima.'
        }
    }

    if ($ComIA -and $ProvedorIA -eq 'ollama') {
        $modelName = if ([string]::IsNullOrWhiteSpace($env:OLLAMA_MODEL)) { 'qwen2.5:1.5b' } else { $env:OLLAMA_MODEL.Trim() }
        if ($modelName -notmatch '^[A-Za-z0-9][A-Za-z0-9._:/-]*$') {
            throw 'OLLAMA_MODEL deve conter um nome de modelo valido, sem espacos ou parametros adicionais.'
        }
        $ollamaContainerId = Get-OllamaContainerId
        if (Test-RabbitHealthy $ollamaContainerId) {
            Write-Host 'Ollama do projeto ja esta pronto; reutilizando.'
        } else {
            if ([string]::IsNullOrWhiteSpace($ollamaContainerId) -and (Test-LocalPort 11434)) {
                throw 'A porta 11434 ja esta ocupada por outro servico. Encerre essa execucao antes de iniciar o Ollama deste projeto.'
            }
            Write-Host 'Preparando a IA local com Ollama...'
            & $script:dockerExecutable compose -p rotaleve -f $composeFile --profile ia up -d ollama
            if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel iniciar o Ollama. Confira o Docker Desktop e a mensagem acima.' }
            $ollamaWait = [System.Diagnostics.Stopwatch]::StartNew()
            $lastOllamaMessage = 0
            do {
                $ollamaContainerId = Get-OllamaContainerId
                if (Test-RabbitHealthy $ollamaContainerId) { break }
                if ($ollamaWait.Elapsed.TotalSeconds -ge 90) {
                    throw 'O Ollama ainda nao ficou disponivel. Consulte: docker compose -p rotaleve --profile ia logs ollama. Depois tente novamente.'
                }
                if ($ollamaWait.Elapsed.TotalSeconds -ge ($lastOllamaMessage + 15)) {
                    Write-Host 'Aguardando a IA local ficar pronta...'
                    $lastOllamaMessage = [int]$ollamaWait.Elapsed.TotalSeconds
                }
                Start-Sleep -Seconds 2
            } while ($true)
        }
        $installedModels = Invoke-CapturedCommand -FilePath $script:dockerExecutable -Arguments @('exec', $ollamaContainerId, 'ollama', 'list')
        if ($installedModels.ExitCode -ne 0) { throw 'Nao foi possivel consultar os modelos da IA local.' }
        $modelAvailable = @($installedModels.Output -split '\r?\n' | Where-Object {
            $_.Trim() -and (($_.Trim() -split '\s+')[0] -ceq $modelName)
        }).Count -gt 0
        if (-not $modelAvailable) {
            Write-Host "Baixando o modelo $modelName para uso local. Aguarde a conclusao..."
            & $script:dockerExecutable exec $ollamaContainerId 'ollama' 'pull' $modelName
            if ($LASTEXITCODE -ne 0) { throw 'O download do modelo nao terminou. Confira a conexao com a internet e tente novamente.' }
        } else {
            Write-Host "Modelo local $modelName disponivel."
        }
        $env:OLLAMA_BASE_URL = 'http://127.0.0.1:11434'
        $env:OLLAMA_MODEL = $modelName
        $env:AI_ENABLED = 'true'
        $env:AI_CHAT_MODEL = 'ollama'
        Write-Host 'Spring AI habilitado com modelo local. Nenhuma chave externa e necessaria.'
    } elseif ($ComIA -and $ProvedorIA -eq 'openai') {
        if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) {
            Write-Host 'A chave sera usada apenas nesta execucao. Nao sera mostrada nem gravada em arquivo.'
            $secureApiKey = Read-Host 'Cole sua chave da OpenAI' -AsSecureString
            $keyPointer = [IntPtr]::Zero
            try {
                $keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureApiKey)
                [Environment]::SetEnvironmentVariable('OPENAI_API_KEY', [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer), 'Process')
            } finally {
                if ($keyPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer) }
                $secureApiKey.Dispose()
            }
            if ([string]::IsNullOrWhiteSpace($env:OPENAI_API_KEY)) { throw 'Nenhuma chave foi informada. Execute sem -ComIA para continuar com a explicacao local.' }
        }
        $env:AI_ENABLED = 'true'
        $env:AI_CHAT_MODEL = 'openai'
        Write-Host 'Spring AI habilitado com o provedor OpenAI para esta execucao.'
    } else {
        Write-Host 'Explicacao local habilitada. A IA generativa pode ser ativada depois.'
    }

    Write-Host "Iniciando a API. Apos a mensagem Started, abra http://localhost:$apiPort/swagger-ui/index.html"
    Write-Host 'Painel RabbitMQ: http://localhost:15672'
    if ($ComIA -and $ProvedorIA -eq 'ollama') { Write-Host 'Modelo de IA: Ollama local em http://localhost:11434' }
    Write-Host 'Para encerrar a API, pressione Ctrl+C. O RabbitMQ continua disponivel.'
    & $javaExecutable '-jar' $jarFile
    if ($LASTEXITCODE -ne 0) { $scriptExitCode = $LASTEXITCODE }
} catch {
    Write-Host ("Nao foi possivel continuar: " + $_.Exception.Message) -ForegroundColor Red
    $scriptExitCode = 1
} finally {
    foreach ($variableName in $changedVariables) {
        [Environment]::SetEnvironmentVariable($variableName, $previousEnvironment[$variableName], 'Process')
    }
    Set-Location -LiteralPath $previousLocation.Path
}
exit $scriptExitCode
