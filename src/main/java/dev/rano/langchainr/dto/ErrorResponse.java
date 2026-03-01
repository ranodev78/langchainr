package dev.rano.langchainr.dto;

import org.springframework.http.HttpStatusCode;

import java.util.List;

public record ErrorResponse(HttpStatusCode httpStatusCode, List<String> errors) {
}
