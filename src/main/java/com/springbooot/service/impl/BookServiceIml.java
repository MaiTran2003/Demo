package com.springbooot.service.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.springbooot.dto.request.BookRequest;
import com.springbooot.dto.response.BookResponse;
import com.springbooot.dto.response.ErrorResponse;
import com.springbooot.entities.Book;
import com.springbooot.repository.BookRepository;
import com.springbooot.service.BookService;

import io.jsonwebtoken.io.IOException;

@Service
public class BookServiceIml implements BookService {

    private final BookRepository bookRepository;
    
    public BookServiceIml(BookRepository bookRepository) {
        super();
        this.bookRepository = bookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Specification<Book> spec = (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("author")), "%" + keyword.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("isbn")), "%" + keyword.toLowerCase() + "%"));

        Page<Book> bookPage = bookRepository.findAll(spec, pageable);
        List<BookResponse> bookResponses = bookPage.getContent().stream()
                .map(this::mapToBookResponse)
                .collect(Collectors.toList());

        return new PageImpl<BookResponse>(bookResponses, pageable, bookPage.getTotalElements());
    }

    @Override
    @Transactional
    public List<BookResponse> createBooks(List<BookRequest> bookRequests) {
        List<BookResponse> createdBookResponses = new ArrayList<>();
        for (BookRequest bookRequest : bookRequests) {
            Book book = new Book();
            book.setAuthor(bookRequest.getAuthor());
            book.setIsbn(bookRequest.getIsbn());
            book.setQuantity(bookRequest.getQuantity());
            book.setTitle(bookRequest.getTitle());
            Book createdBook = bookRepository.save(book);
            createdBookResponses.add(mapToBookResponse(createdBook));
        }
        return createdBookResponses;
    }

    @Override
    @Transactional
    public BookResponse updateBook(Long id, BookRequest bookRequests) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));

        book.setTitle(bookRequests.getTitle());
        book.setAuthor(bookRequests.getAuthor());
        book.setIsbn(bookRequests.getIsbn());
        book.setQuantity(bookRequests.getQuantity());

        Book updatedBook = bookRepository.save(book);
        return mapToBookResponse(updatedBook);
    }

    @Override
    @Transactional
    public BookResponse deleteBook(Long id) {
         bookRepository.deleteById(id);
         return null;
    }
    
    private BookResponse mapToBookResponse(Book book) {
        BookResponse bookResponse = new BookResponse();
        bookResponse.setTitle(book.getTitle());
        bookResponse.setAuthor(book.getAuthor());
        bookResponse.setIsbn(book.getIsbn());
        bookResponse.setQuantity(book.getQuantity());
        /**
         * Map other properties as needed
         */
        return bookResponse;
    }

    @Override
    @Transactional
    public List<Book> csvToBooks(InputStream inputStream) {
        List<Book> books = new ArrayList<>();
        List<ErrorResponse> errorResponses = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            com.opencsv.CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
            CSVReader csvReader = new CSVReaderBuilder(fileReader).withCSVParser(parser).build();

            /**
             * Read headers from the CSV file
             */
            String[] headers = csvReader.readNext();

            if (headers != null) {
                String[] values;
                while ((values = csvReader.readNext()) != null) {
                    Book book = new Book();
                    for (int i = 0; i < headers.length; i++) {
                        String header = headers[i].toLowerCase();
                        String value = values[i];

                        try {
                            switch (header) {
                                case "id":
                                    if (value != null && !value.isEmpty()) {
                                        Long id = Long.parseLong(value);
                                        if (id > 0) {
                                            book.setId(id);
                                        } else {
                                            errorResponses.add(new ErrorResponse("Invalid value for " + header + ": " + value + ". ID must be a positive integer."));
                                        }
                                    }
                                    break;
                                case "author":
                                    book.setAuthor(value);
                                    break;
                                case "isbn":
                                    book.setIsbn(value);
                                    break;
                                case "quantity":
                                    if (value != null && !value.isEmpty()) {
                                        Integer quantity = Integer.parseInt(value);
                                        if (quantity >= 0) {
                                            book.setQuantity(quantity);
                                        } else {
                                            errorResponses.add(new ErrorResponse("Invalid value for " + header + ": " + value + ". Quantity must be a non-negative integer."));
                                        }
                                    }
                                    break;
                                case "title":
                                    book.setTitle(value);
                                    break;
                                default:
                                    errorResponses.add(new ErrorResponse("Invalid header: " + header));
                                    break;
                            }
                        } catch (NumberFormatException e) {
                            errorResponses.add(new ErrorResponse("Invalid format for " + header + ": " + value + ". Please provide a valid format."));
                        } catch (Exception e) {
                            errorResponses.add(new ErrorResponse("Error processing " + header + ": " + e.getMessage()));
                        }
                    }
                    books.add(book);
                }
            } else {
                errorResponses.add(new ErrorResponse("No data found in CSV file"));
            }
        } catch (IOException | java.io.IOException e) {
            e.printStackTrace();
            errorResponses.add(new ErrorResponse("Error processing CSV file: " + e.getMessage()));
        }
        return books;
    }

    @Override
    @Transactional
    public ErrorResponse processAndSaveData(MultipartFile file) {
        ErrorResponse errorResponse = new ErrorResponse();
        try {
            if (!hasCsvFormat(file)) {
                errorResponse.setMessage("File is not in CSV format");
                return errorResponse;
            }
            if (!hasValidSize(file)) {
                errorResponse.setMessage("File size exceeds the maximum allowed limit (5MB)");
                return errorResponse;
            }

            /**
             * Process and save data from the CSV file
             */
            List<Book> books = csvToBooks(file.getInputStream());
            if (books != null && !books.isEmpty()) {
                bookRepository.saveAll(books);
                errorResponse.setMessage("Data processed and saved successfully");
            } else {
                errorResponse.setMessage("No data found in CSV file");
            }
            return errorResponse;
        } catch (IOException | java.io.IOException e) {
            e.printStackTrace();
            errorResponse.setMessage("Error processing CSV file: " + e.getMessage());
            return errorResponse;
        }
    }
   
    public boolean hasValidSize(MultipartFile file) {
        long maxSizeInBytes = 5 * 1024 * 1024; // 5MB
        return file.getSize() <= maxSizeInBytes;
    }

    public boolean hasCsvFormat(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        String type = "text/csv";
        if (!type.equals(file.getContentType()) && fileName != null && fileName.toLowerCase().endsWith(".csv"))
            return false;
        return true;
    }
}
