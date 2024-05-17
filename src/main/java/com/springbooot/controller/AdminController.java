package com.springbooot.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.springbooot.dto.request.BookRequest;
import com.springbooot.dto.request.SignOutRequest;
import com.springbooot.dto.response.BookResponse;
import com.springbooot.dto.response.MessageResponse;
import com.springbooot.dto.response.UserResponse;
import com.springbooot.service.AuthenticationService;
import com.springbooot.service.BookService;
import com.springbooot.service.JwtService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
	
	private final BookService bookService;

	private final JwtService jwtService;

	private final AuthenticationService authenticationService;
	
	public AdminController(BookService bookService, JwtService jwtService,
			AuthenticationService authenticationService) {
		super();
		this.bookService = bookService;
		this.jwtService = jwtService;
		this.authenticationService = authenticationService;
	}

	@GetMapping
	public ResponseEntity<String> sayHello(){
		return ResponseEntity.ok("Hi Admin");
	}
	
	@PostMapping("/logout")
	public ResponseEntity<?> signout(@RequestBody SignOutRequest signOutRequest, @RequestHeader("Authorization") String header) {
	    String token = jwtService.extractTokenFromHeader(header);
	    if (token == null) {
	        return ResponseEntity.badRequest().body("Token not found in Authorization header.");
	    }
	    try {
	        /**
	         * Check if the token has been logged out before
	         */
	        if (jwtService.isTokenLoggedOut(signOutRequest.getEmail(), token)) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token has already been logged out.");
	        }
	        UserResponse user = authenticationService.logout(signOutRequest, token);
	        return ResponseEntity.ok("User " + user.getEmail() + " has been logged out successfully.");
	    } catch (RuntimeException e) {
	        return ResponseEntity.badRequest().body("Logout failed! " + e.getMessage());
	    }
	}
	@GetMapping("/search")
	public ResponseEntity<Page<BookResponse>> searchBooks(@RequestParam String keyword, 
	                                                @RequestParam(defaultValue = "0") int page, 
	                                                @RequestParam(defaultValue = "10") int size) {
	    Page<BookResponse> books = bookService.searchBooks(keyword, page, size);
	    return ResponseEntity.ok(books);
	}
	
	@PostMapping("/books")
	public ResponseEntity<List<BookResponse>> createBooks(@RequestBody List<BookRequest> bookRequestList) {
	    List<BookResponse> createdBooks = bookService.createBooks(bookRequestList);
	    return ResponseEntity.status(HttpStatus.CREATED).body(createdBooks);
	}


	@PutMapping("/books/{id}")
	public ResponseEntity<BookResponse> updateBook(@PathVariable Long id, @RequestBody BookRequest bookRequest) {
	    BookResponse updatedBook = bookService.updateBook(id, bookRequest);
	    return ResponseEntity.ok(updatedBook);
	}

	@DeleteMapping("/books/{id}")
	public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
	    bookService.deleteBook(id);
	    return ResponseEntity.noContent().build();
	}

	@PostMapping("/import")
	public ResponseEntity<MessageResponse> importBooks(@RequestParam("file") MultipartFile file) {
	        if (bookService.hasCsvFormat(file) && bookService.hasValidSize(file)) {
	        	bookService.processAndSaveData(file);
	        
	        return ResponseEntity.status(HttpStatus.OK)
	                .body(new MessageResponse("Uploaded the file successfully: " + file.getOriginalFilename()));
	        }
	        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
	                .body(new MessageResponse("Please upload CSV file"));
	}
	@GetMapping("/search_user")
	public ResponseEntity<Page<UserResponse>> searchUsers(@RequestParam String keyword, 
	                                                @RequestParam(defaultValue = "0") int page, 
	                                                @RequestParam(defaultValue = "10") int size) {
	    Page<UserResponse> users = authenticationService.searchUsers(keyword, page, size);
	    return ResponseEntity.ok(users);
	}
	
}
