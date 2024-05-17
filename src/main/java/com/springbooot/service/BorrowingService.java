package com.springbooot.service;


import com.springbooot.dto.request.BorrowBookRequest;
import com.springbooot.dto.request.ReturnBookRequest;
import com.springbooot.dto.response.ErrorResponse;

public interface BorrowingService {
	
	ErrorResponse borrowBook(BorrowBookRequest borrowBookRequest);
	
	ErrorResponse returnBook(ReturnBookRequest returnBookRequest);

}
