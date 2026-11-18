package br.com.fiap.rotaleve.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail negocio(RegraNegocioException exception) { return erro(exception.getStatus(), exception.getMessage()); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    ProblemDetail status(org.springframework.web.server.ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(),
            exception.getReason() == null ? "Não foi possível concluir a operação." : exception.getReason());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException exception) {
        var campos = new TreeMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(e -> campos.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = erro(HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        problem.setProperty("erros", campos); return problem;
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class, PropertyReferenceException.class})
    ProblemDetail entrada(Exception exception) { return erro(HttpStatus.BAD_REQUEST, "Requisição inválida. Confira os tipos, valores e campos obrigatórios."); }
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail autenticacao(AuthenticationException exception) { return erro(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."); }
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acesso(AccessDeniedException exception) { return erro(HttpStatus.FORBIDDEN, "Seu usuário não pode acessar este recurso."); }
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    ProblemDetail integridade(Exception exception) { return erro(HttpStatus.CONFLICT, "A operação conflita com dados existentes. O recurso pode estar vinculado a outros registros ou já ter sido alterado."); }
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail inexistente(NoResourceFoundException exception) { return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado."); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ProblemDetail metodo(Exception exception) { return erro(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não permitido para este recurso."); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ProblemDetail formato(Exception exception) { return erro(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato de conteúdo não suportado para este recurso."); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ProblemDetail tamanho(Exception exception) { return erro(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 5 MB."); }
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    ProblemDetail arquivoAusente(Exception exception) { return erro(HttpStatus.BAD_REQUEST, "Envie a foto no campo arquivo, usando multipart/form-data."); }
    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception exception) {
        log.error("Falha não prevista ao processar requisição: {}", exception.getClass().getSimpleName());
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a operação.");
    }
    private ProblemDetail erro(HttpStatus status, String mensagem) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, mensagem);
        problem.setTitle(switch (status) {
            case BAD_REQUEST -> "Dados inválidos";
            case UNAUTHORIZED -> "Autenticação necessária";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Recurso não encontrado";
            case CONFLICT -> "Conflito de dados";
            default -> status.getReasonPhrase();
        }); return problem;
    }
}
