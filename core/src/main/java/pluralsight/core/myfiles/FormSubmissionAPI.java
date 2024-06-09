package pluralsight.core.myfiles;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org. apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org. apache. sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;

import static org.apache.sling.api.servlets.ServletResolverConstants.*;
@Component (service = Servlet.class,
        property = {
                SLING_SERVLET_METHODS +"=POST" ,
                SLING_SERVLET_PATHS +"=/bin/form-submission",
                SLING_SERVLET_EXTENSIONS + "=json"})

public class FormSubmissionAPI extends SlingAllMethodsServlet {

    @Reference
    private ConfigurationAdmin configAdmin;

    private final Logger log = LoggerFactory.getLogger (FormSubmissionAPI.class);

    protected void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws IOException {
        try {
            if (validateCaptcha(request)) {
                if
                (postMuleSoft(request, response)) {
                    response.setStatus(200);
                    response.getWriter().println("Form Success");
                } else {
                    response.setStatus(500);
                    response.getWriter().println("Backend error - Form data not saved");
                }
            } else {
                response.setStatus(500);
                response.getWriter().println("unable to validate");
            }
        } catch
        (Exception e) {
            log.error("Error at Form Submission-", e);
            response.setStatus(500);
            response.getWriter().println("Internal Error- " + e);
        }
    }

    protected boolean validateCaptcha(SlingHttpServletRequest request) throws NullPointerException, IOException {

                    final String token = request.getParameter("token");
                    final String secretKey = "12uruwb1214324r904tgherignr";

                    final HttpUriRequest googleRequest = RequestBuilder.post()
                            .setUri("https://www.google.com/recaptcha/api/siteverify")
                            .addParameter("secret", secretKey)
                            .addParameter("response", token)
                            .build();

                    final HttpClient httpClient = null;
                    final HttpResponse googleResponse = httpClient.execute(googleRequest);
                    return googleResponse.getStatusLine().getStatusCode() == 200;
    }

        protected boolean postMuleSoft(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
//Get MuleSoft Endpoint from OSGi configs
            Configuration conf = configAdmin.getConfiguration("com.backend.configs");
            if (conf == null) {
                response.setStatus(500);
                response.getWriter().println("Backend configs not found..");
                return false;
            }

            Dictionary<String, Object> props = conf.getProperties();

            if (props.get ("muleSoftUrl") == null || props.get("Authorization") == null) {
                response. setStatus(500);
                response. getWriter().println("Missing Backend configs..");
                return false;
            }
//Read input form data
            BufferedReader reader = request.getReader();
            StringBuilder formData = new StringBuilder();
            String line = reader.readLine();
            formData.append (line);

//response getWriter() .println("This is fed form- "+formData. toString(+"xyz");
//Post to Mulesoft
                HttpPost httpPost = new HttpPost(props.get ("muleSoftUrl").toString());
                httpPost.setHeader( "Content-Type",  "application/json");
                httpPost.setHeader (  "Authorization", props.get("Authorization"). toString());
                httpPost. setEntity(new StringEntity(formData.toString()));
                CloseableHttpResponse httpResponse;
                try (CloseableHttpClient client = HttpClients.custom()
                        .disableAutomaticRetries()
                        .disableConnectionState()
                        .disableContentCompression()
                        .disableRedirectHandling()
                        .useSystemProperties()
                        .build()){
                    httpResponse = client.execute(httpPost);
                    String muleResponse = IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8);
                    if (httpResponse.getStatusLine().getStatusCode() == 200 && muleResponse.contains("Success")) {
                        return true;
                    } else {
                        log.error("Mulesoft response- {}", muleResponse);
                        response.setStatus(200);
                        response.getWriter().println("Error- " + muleResponse);
                        return false;
                    }
                }
            }
        }


