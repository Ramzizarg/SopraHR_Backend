package com.example.contactservice.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.contactservice.model.ContactRequest;
import com.example.contactservice.repository.ContactRequestRepository;

@Service
@Transactional
public class ContactService {

    private final ContactRequestRepository contactRequestRepository;

    @Autowired
    public ContactService(ContactRequestRepository contactRequestRepository) {
        this.contactRequestRepository = contactRequestRepository;
    }

    public ContactRequest createContactRequest(ContactRequest contactRequest, String employeeName) {
        contactRequest.setCreatedAt(LocalDateTime.now());
        contactRequest.setEmployeeName(employeeName);
        return contactRequestRepository.save(contactRequest);
    }

    public List<ContactRequest> getAllContactRequests() {
        return contactRequestRepository.findAll();
    }

    public ContactRequest getContactRequestById(Long id) {
        return contactRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact request not found"));
    }

    public ContactRequest updateContactRequestStatus(Long id, String status) {
        ContactRequest contactRequest = getContactRequestById(id);
        contactRequest.setStatus(status);
        return contactRequestRepository.save(contactRequest);
    }

    public ContactRequest respondToContactRequest(Long id, String response, String status) {
        ContactRequest request = getContactRequestById(id);
        request.setResponse(response);
        request.setStatus(status);
        request.setRespondedAt(LocalDateTime.now());
        return contactRequestRepository.save(request);
    }

    public void deleteContactRequest(Long id) {
        contactRequestRepository.deleteById(id);
    }
} 