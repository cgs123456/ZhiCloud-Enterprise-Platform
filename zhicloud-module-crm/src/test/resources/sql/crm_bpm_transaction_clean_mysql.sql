DELETE FROM crm_receivable;
DELETE FROM bpm_process_instance_idempotency
WHERE process_definition_key = 'crm-receivable-audit';
