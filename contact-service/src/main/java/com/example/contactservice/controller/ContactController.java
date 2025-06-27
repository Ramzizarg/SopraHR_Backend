package com.example.contactservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.contactservice.model.ContactRequest;
import com.example.contactservice.service.ContactService;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
public class ContactController {

    private final ContactService contactService;

    @Autowired
    public ContactController(ContactService contactService) {
        super();
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactRequest> createContactRequest(
            @RequestBody ContactRequest contactRequest,
            @RequestHeader(value = "Employee-Name", required = false) String employeeName) {
        return ResponseEntity.ok(contactService.createContactRequest(contactRequest, employeeName));
    }

    @GetMapping
    public ResponseEntity<List<ContactRequest>> getAllContactRequests() {
        return ResponseEntity.ok(contactService.getAllContactRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactRequest> getContactRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactRequestById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ContactRequest> updateContactRequestStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(contactService.updateContactRequestStatus(id, status));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<ContactRequest> respondToContactRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String response = request.get("response");
        String status = request.get("status");
        return ResponseEntity.ok(contactService.respondToContactRequest(id, response, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContactRequest(@PathVariable Long id) {
        contactService.deleteContactRequest(id);
        return ResponseEntity.ok().build();
    }
} 