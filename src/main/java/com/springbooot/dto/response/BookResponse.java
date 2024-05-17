package com.springbooot.dto.response;

public class BookResponse {
    
    private Long id;

    private String title;
    
    private String author;
    
    private String isbn;
    
    private int quantity;

    public BookResponse() { }

    public BookResponse(Long id, String title, String author, String isbn, int quantity) {
      this.id = id;
      this.title = title;
      this.author = author;
      this.isbn = isbn;
      this.quantity = quantity;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getAuthor() {
      return author;
    }

    public void setAuthor(String author) {
      this.author = author;
    }

    public String getIsbn() {
      return isbn;
    }

    public void setIsbn(String isbn) {
      this.isbn = isbn;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }
}
