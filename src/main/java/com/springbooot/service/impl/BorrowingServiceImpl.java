package com.springbooot.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springbooot.dto.request.BorrowBookRequest;
import com.springbooot.dto.request.ReturnBookRequest;
import com.springbooot.dto.response.ErrorResponse;
import com.springbooot.entities.Book;
import com.springbooot.entities.Borrowing;
import com.springbooot.entities.User;
import com.springbooot.repository.BookRepository;
import com.springbooot.repository.BorrowingRepository;
import com.springbooot.repository.UserRepository;
import com.springbooot.service.BorrowingService;

import java.sql.Date;
import java.util.Optional;

@Service
public class BorrowingServiceImpl implements BorrowingService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BorrowingRepository borrowingRepository;

    public BorrowingServiceImpl(BookRepository bookRepository, UserRepository userRepository,
                                BorrowingRepository borrowingRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.borrowingRepository = borrowingRepository;
    }

    @Override
    @Transactional
    public ErrorResponse borrowBook(BorrowBookRequest borrowBookRequest) {
        ErrorResponse errorResponse = new ErrorResponse();

        Long bookId = borrowBookRequest.getBookId();
        Long userId = borrowBookRequest.getUserId();
        
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isEmpty()) {
            errorResponse.setMessage("Book not found with id: " + bookId);
            return errorResponse;
        }
        Book book = optionalBook.get();
        
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            errorResponse.setMessage("User not found with id: " + userId);
            return errorResponse;
        }
        User user = optionalUser.get();

        /**
         * Check if the book or user is not found
         */
        if (book == null) {
            errorResponse.setMessage("Book not found with id: " + bookId);
            return errorResponse;
        }
        if (user == null) {
            errorResponse.setMessage("User not found with id: " + userId);
            return errorResponse;
        }

        /**
         * Check if the quantity of the book in stock is sufficient for borrowing
         */
        if (book.getQuantity() <= 0) {
            errorResponse.setMessage("The book is out of stock.");
            return errorResponse;
        }

        /**
         * Check if the user has previously borrowed this book
         */
        Optional<Borrowing> existingBorrowing = borrowingRepository.findByUserAndBookAndReturnDateIsNull(user, book);
        if (existingBorrowing.isPresent()) {
            errorResponse.setMessage("You have already borrowed this book.");
            return errorResponse;
        }

        /**
         * Borrow the book by decreasing the quantity in stock and adding information to the Borrowing table
         */
        book.setQuantity(book.getQuantity() - 1);
        bookRepository.save(book);

        Borrowing borrowing = new Borrowing();
        borrowing.setUser(user);
        borrowing.setBook(book);
        borrowing.setBorrowDate(new Date(System.currentTimeMillis()));
        borrowingRepository.save(borrowing);

        return null;
    }

    @Override
    @Transactional
    public ErrorResponse returnBook(ReturnBookRequest returnBookRequest) {
        ErrorResponse errorResponse = new ErrorResponse();

        Long borrowingId = returnBookRequest.getBorrowingId();

        Borrowing borrowing = borrowingRepository.findById(borrowingId)
                .orElse(null);

        /**
         * Check if the borrowing record is not found
         */
        if (borrowing == null) {
            errorResponse.setMessage("Borrowing not found with id: " + borrowingId);
            return errorResponse;
        }

        /**
         * Check if the book has already been returned
         */
        if (borrowing.getReturnDate() != null) {
            errorResponse.setMessage("This book has already been returned.");
            return errorResponse;
        }

        /**
         * Return the book by increasing the quantity in stock and updating the return date in the Borrowing table
         */
        Book book = borrowing.getBook();
        book.setQuantity(book.getQuantity() + 1);
        bookRepository.save(book);

        borrowing.setReturnDate(new Date(System.currentTimeMillis()));
        borrowingRepository.save(borrowing);

        return null;
    }
}
