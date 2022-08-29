package software.amazon.m2.common;

import org.apache.commons.lang3.Validate;
import software.amazon.awssdk.services.m2.model.AccessDeniedException;
import software.amazon.awssdk.services.m2.model.ConflictException;
import software.amazon.awssdk.services.m2.model.InternalServerException;
import software.amazon.awssdk.services.m2.model.M2Exception;
import software.amazon.awssdk.services.m2.model.ResourceNotFoundException;
import software.amazon.awssdk.services.m2.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.m2.model.ThrottlingException;
import software.amazon.awssdk.services.m2.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.function.Supplier;

/**
 * Convert M2 exceptions to Cfn exceptions for an M2 service call.
 */
public final class ExceptionHandlerWrapper {

    private ExceptionHandlerWrapper() {
    }

    public static <T> T wrapM2Exception(final String operation, final Supplier<T> serviceCall) {
        Validate.notBlank(operation);
        Validate.notNull(serviceCall);

        try {
            return serviceCall.get();
        } catch (final ConflictException ex) {
            throw new CfnAlreadyExistsException(ex);
        } catch (final ResourceNotFoundException ex) {
            throw new CfnNotFoundException(ex);
        } catch (final ServiceQuotaExceededException ex) {
            throw new CfnServiceLimitExceededException(ex);
        } catch (final ValidationException ex) {
            throw new CfnInvalidRequestException(ex);
        } catch (final InternalServerException ex) {
            throw new CfnInternalFailureException(ex);
        } catch (AccessDeniedException ex) {
            throw new CfnAccessDeniedException(operation, ex);
        } catch (ThrottlingException ex) {
            throw new CfnThrottlingException(ex);
        } catch (final M2Exception ex) {
            throw new CfnGeneralServiceException(operation, ex);
        }
    }
}
