package com.example.azureAD;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class AzureAdApplication {

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	@Value("${spring.cloud.azure.active-directory.credential.client-id}")
	private String clientId;

	@Value("${spring.cloud.azure.active-directory.credential.client-secret}")
	private String clientSecret;

	@Value("${spring.cloud.azure.active-directory.profile.tenant-id}")
	private String tenantId;

	@Value("${SampleKV}")
	private String sampleKV;

	@GetMapping("/welcome")
	public String sayHi(){
		return "Welcome from Azure Active Directory! Data from pipeline: " + sampleKV;
	}

	@GetMapping("/other")
	public String saySthElse(){
		return "Other endpoint";
	}

	@GetMapping("/user-info")
	public List<List<User>> getUserInfo(OAuth2AuthenticationToken authentication){

		final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
				.clientId(clientId)
				.clientSecret(clientSecret)
				.tenantId(tenantId)
				.build();

		final TokenCredentialAuthProvider tokenCredAuthProvider =
				new TokenCredentialAuthProvider(Arrays.asList("https://graph.microsoft.com/.default"), clientSecretCredential);


		final GraphServiceClient graphClient = GraphServiceClient
				.builder()
				.authenticationProvider(tokenCredAuthProvider)
				.buildClient();

		OAuth2AuthorizedClient client = authorizedClientService
				.loadAuthorizedClient(
						authentication.getAuthorizedClientRegistrationId(),
						authentication.getName());

		UserCollectionPage users = graphClient.users().buildRequest().get();

		List<List<User>> allUserList = new ArrayList<>();

		do {
			List<User> currentPageUser = users.getCurrentPage();
			Collections.addAll(allUserList, currentPageUser);
			UserCollectionRequestBuilder nextPage = users.getNextPage();
			users = nextPage == null ? null : nextPage.buildRequest().get();
		} while (users != null);

		return allUserList;
	}




	public static void main(String[] args) {
		SpringApplication.run(AzureAdApplication.class, args);
	}

}
