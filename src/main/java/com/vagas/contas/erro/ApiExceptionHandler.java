package com.vagas.contas.erro;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Transforma qualquer erro em uma resposta JSON simples:
 * { "erro": "mensagem para a pessoa", "campos": { "senha": "..." } }
 * Assim o front sempre sabe onde achar a mensagem.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record Erro(String erro, Map<String, String> campos) {
    }

    /** Erros que nós mesmos lançamos (ex.: e-mail já cadastrado). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Erro> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new Erro(e.getMessage(), Map.of()));
    }

    /** Campos inválidos (ex.: senha curta, e-mail sem @). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new Erro("Confira os campos destacados.", campos));
    }

    /** JSON mal formatado ou com tipo errado (ex.: texto onde devia ser lista). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Erro> jsonInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(new Erro("Os dados enviados estão em um formato inválido.", Map.of()));
    }

    /** O banco recusou algum valor (ex.: opção que não existe no cadastro). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erro> banco(DataIntegrityViolationException e) {
        log.warn("Banco recusou os dados: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest()
                .body(new Erro("Algum valor enviado não é aceito. Confira as opções do cadastro.", Map.of()));
    }

    /** Parâmetro com formato errado (ex.: id de vaga que não é um UUID). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Erro> tipoErrado(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new Erro("Parâmetro inválido: " + e.getName() + ".", Map.of()));
    }

    /** Arquivo maior que o limite (spring.servlet.multipart.max-file-size). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Erro> arquivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new Erro("O arquivo é grande demais.", Map.of()));
    }

    /** Envio de arquivo sem o campo "arquivo" ou fora do formato multipart. */
    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<Erro> envioDeArquivoInvalido(Exception e) {
        return ResponseEntity.badRequest()
                .body(new Erro("Envie o arquivo no campo \"arquivo\" (formato multipart/form-data).", Map.of()));
    }

    /** Qualquer outro erro inesperado. O detalhe fica só no log, nunca vai para o site. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Erro> inesperado(Exception e) {
        // Erros "normais" do Spring (rota inexistente, método errado...) mantêm o status original
        if (e instanceof ErrorResponse respostaPadrao) {
            return ResponseEntity.status(respostaPadrao.getStatusCode())
                    .body(new Erro("Requisição inválida para esta rota.", Map.of()));
        }
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Erro("Algo deu errado do nosso lado. Tente de novo em instantes.", Map.of()));
    }
}
