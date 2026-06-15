package pk.ls.pasir.sosnowski_lukasz.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionResolver {

    @Override
    public @NonNull Mono<List<GraphQLError>> resolveException(
            @NonNull Throwable ex,
            @NonNull DataFetchingEnvironment env
    ) {
        if (ex instanceof ConstraintViolationException validationEx) {
            List<GraphQLError> errors = validationEx.getConstraintViolations().stream()
                    .map(violation -> GraphqlErrorBuilder.newError(env)
                            .message(violation.getMessage())
                            .build())
                    .toList();

            return Mono.just(errors);
        }

        if (ex instanceof BindException bindEx) {
            List<GraphQLError> errors = bindEx.getBindingResult().getFieldErrors().stream()
                    .map(error -> GraphqlErrorBuilder.newError(env)
                            .message(error.getField() + ": " + error.getDefaultMessage())
                            .build())
                    .toList();

            return Mono.just(errors);
        }

        if (ex instanceof EntityNotFoundException
                || ex instanceof AccessDeniedException
                || ex instanceof IllegalArgumentException) {
            return Mono.just(List.of(
                    GraphqlErrorBuilder.newError(env)
                            .message(ex.getMessage())
                            .build()
            ));
        }

        return Mono.just(List.of(
                GraphqlErrorBuilder.newError(env)
                        .message(ex.getMessage() != null ? ex.getMessage() : "Nieznany blad")
                        .build()
        ));
    }
}