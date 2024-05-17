package com.springbooot.service;

import java.io.InputStream;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.springbooot.dto.request.BookRequest;
import com.springbooot.dto.response.BookResponse;
import com.springbooot.dto.response.ErrorResponse;
import com.springbooot.entities.Book;

@Service
public interface BookService {
  
  Page<BookResponse> searchBooks(String keyword, int page, int size);
  
  List<BookResponse> createBooks(List<BookRequest> bookRequests);
  
  BookResponse updateBook(Long id, BookRequest bookRequests);
  
  BookResponse deleteBook(Long id);
  
  ErrorResponse processAndSaveData(MultipartFile file);
  
  List<Book> csvToBooks(InputStream inputStream);

boolean hasValidSize(MultipartFile file);

boolean hasCsvFormat(MultipartFile file);
}
